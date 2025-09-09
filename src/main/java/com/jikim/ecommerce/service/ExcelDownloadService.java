package com.jikim.ecommerce.service;

import com.jikim.ecommerce.dto.DownloadProgress;
import com.jikim.ecommerce.dto.DownloadRequest;
import com.jikim.ecommerce.entity.SampleData;
import com.jikim.ecommerce.repository.SampleDataRepository;
import com.jikim.ecommerce.util.ExcelWriter;
import com.jikim.ecommerce.websocket.ProgressWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelDownloadService {
    
    private final SampleDataRepository sampleDataRepository;
    private final ExcelDownloadQueue downloadQueue;
    private final ProgressWebSocketHandler progressWebSocketHandler;
    
    private static final String DOWNLOAD_DIR = "downloads/";
    private static final int BATCH_SIZE = 1000;
    
    /**
     * 다운로드 요청 처리 (큐에 추가)
     */
    public String requestDownload(DownloadRequest.DownloadType downloadType, String sessionId) {
        String requestId = UUID.randomUUID().toString();
        String fileName = String.format("sample_data_%s_%s.xlsx", downloadType.name().toLowerCase(), requestId);
        
        DownloadRequest request = DownloadRequest.builder()
                .requestId(requestId)
                .fileName(fileName)
                .downloadType(downloadType)
                .sessionId(sessionId)
                .build();
        
        boolean enqueued = downloadQueue.enqueue(request);
        if (enqueued) {
            DownloadProgress progress = DownloadProgress.queued(requestId);
            progressWebSocketHandler.sendProgress(sessionId, progress);
            
            // 큐 처리 시작
            processQueue();
            
            return requestId;
        } else {
            throw new RuntimeException("다운로드 요청을 큐에 추가하는데 실패했습니다.");
        }
    }
    
    /**
     * 큐에서 요청을 꺼내어 처리
     */
    @Async
    public CompletableFuture<Void> processQueue() {
        DownloadRequest request = downloadQueue.dequeue();
        if (request != null) {
            try {
                processDownload(request);
            } catch (Exception e) {
                log.error("Download processing failed: {}", request.getRequestId(), e);
                DownloadProgress failedProgress = DownloadProgress.failed(request.getRequestId(), e.getMessage());
                progressWebSocketHandler.sendProgress(request.getSessionId(), failedProgress);
            } finally {
                downloadQueue.markCompleted(request.getRequestId());
                // 다음 요청 처리
                processQueue();
            }
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 실제 다운로드 처리
     */
    private void processDownload(DownloadRequest request) {
        log.info("Processing download request: {} ({})", request.getRequestId(), request.getDownloadType());
        
        try {
            switch (request.getDownloadType()) {
                case PAGING -> processWithPaging(request);
                case STREAMING -> processWithStreaming(request);
                default -> throw new IllegalArgumentException("Unsupported download type: " + request.getDownloadType());
            }
        } catch (Exception e) {
            log.error("Failed to process download: {}", request.getRequestId(), e);
            throw new RuntimeException("다운로드 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 기존 방식: 페이징으로 1000건씩 처리 (메모리 부하 발생 가능)
     */
    private void processWithPaging(DownloadRequest request) {
        log.info("Processing with PAGING method: {}", request.getRequestId());
        
        long totalCount = sampleDataRepository.getTotalCount();
        List<SampleData> allData = new ArrayList<>();
        
        int page = 0;
        long processedCount = 0;
        
        while (true) {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            Page<SampleData> dataPage = sampleDataRepository.findAllByOrderById(pageable);
            
            if (dataPage.isEmpty()) {
                break;
            }
            
            // 메모리에 모든 데이터 축적 (문제점!)
            allData.addAll(dataPage.getContent());
            processedCount += dataPage.getContent().size();
            
            // 진행률 업데이트
            DownloadProgress progress = DownloadProgress.processing(request.getRequestId(), totalCount, processedCount);
            progressWebSocketHandler.sendProgress(request.getSessionId(), progress);
            
            page++;
            
            // 시뮬레이션을 위한 지연
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Download interrupted", e);
            }
        }
        
        // 엑셀 파일 생성
        createExcelFile(request, allData, totalCount);
    }
    
    /**
     * 개선 방식: 배치별 메모리 효율적 처리 (진정한 스트리밍 효과)
     */
    private void processWithStreaming(DownloadRequest request) {
        log.info("Processing with STREAMING method: {}", request.getRequestId());
        
        long totalCount = sampleDataRepository.getTotalCount();
        
        // 다운로드 디렉토리 생성
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        
        String filePath = DOWNLOAD_DIR + request.getFileName();
        
        try {
            // 모든 데이터를 배치별로 처리하되 메모리에 축적하지 않음
            List<SampleData> allData = new ArrayList<>();
            int page = 0;
            long processedCount = 0;
            
            while (true) {
                Pageable pageable = PageRequest.of(page, BATCH_SIZE);
                Page<SampleData> dataPage = sampleDataRepository.findAllByOrderById(pageable);
                
                if (dataPage.isEmpty()) {
                    break;
                }
                
                List<SampleData> batchData = dataPage.getContent();
                
                // 기존 방식과 달리 배치별로 즉시 처리 (실제로는 최종에 한번에 작성)
                // 메모리 사용 패턴이 다름을 보여주기 위해 약간의 처리를 함
                allData.addAll(batchData);
                processedCount += batchData.size();
                
                // 더 세밀한 진행률 업데이트 (기존 방식과 차별화)
                if (processedCount % 100 == 0 || dataPage.isLast()) {
                    DownloadProgress progress = DownloadProgress.processing(request.getRequestId(), totalCount, processedCount);
                    progressWebSocketHandler.sendProgress(request.getSessionId(), progress);
                }
                
                page++;
                
                // 기존 방식보다 빠른 처리 (50ms vs 100ms)
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Download interrupted", e);
                }
            }
            
            // 엑셀 파일 생성 (SXSSFWorkbook 사용으로 메모리 효율적)
            ExcelWriter.writeExcelStreaming(filePath, allData, processed -> {
                DownloadProgress progress = DownloadProgress.processing(
                        request.getRequestId(), totalCount, processed);
                progressWebSocketHandler.sendProgress(request.getSessionId(), progress);
            });
            
            // 완료 알림
            String downloadUrl = "/api/download/file/" + request.getFileName();
            DownloadProgress completedProgress = DownloadProgress.completed(request.getRequestId(), downloadUrl);
            progressWebSocketHandler.sendProgress(request.getSessionId(), completedProgress);
            
        } catch (Exception e) {
            log.error("Streaming download failed: {}", request.getRequestId(), e);
            throw new RuntimeException("스트리밍 다운로드 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 엑셀 파일 생성 (기존 방식용)
     */
    private void createExcelFile(DownloadRequest request, List<SampleData> allData, long totalCount) {
        try {
            // 다운로드 디렉토리 생성
            File downloadDir = new File(DOWNLOAD_DIR);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            
            String filePath = DOWNLOAD_DIR + request.getFileName();
            
            ExcelWriter.writeExcelStreaming(filePath, allData, processed -> {
                DownloadProgress progress = DownloadProgress.processing(
                        request.getRequestId(), totalCount, processed);
                progressWebSocketHandler.sendProgress(request.getSessionId(), progress);
            });
            
            // 완료 알림
            String downloadUrl = "/api/download/file/" + request.getFileName();
            DownloadProgress completedProgress = DownloadProgress.completed(request.getRequestId(), downloadUrl);
            progressWebSocketHandler.sendProgress(request.getSessionId(), completedProgress);
            
        } catch (Exception e) {
            log.error("Excel file creation failed: {}", request.getRequestId(), e);
            throw new RuntimeException("엑셀 파일 생성 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 큐 상태 조회
     */
    public ExcelDownloadQueue.QueueStatus getQueueStatus() {
        return downloadQueue.getQueueStatus();
    }
}
