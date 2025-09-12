package com.jikim.ecommerce.service;

import com.jikim.ecommerce.dto.DownloadProgress;
import com.jikim.ecommerce.dto.DownloadRequest;
import com.jikim.ecommerce.websocket.ProgressWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class FastExcelDownloadService {
    
    private final JdbcTemplate jdbcTemplate;
    private final ProgressWebSocketHandler progressWebSocketHandler;
    
    private static final String DOWNLOAD_DIR = "downloads/";
    
    /**
     * FastExcel로 대용량 데이터 처리 (단일 쿼리 방식)
     */
    public void processWithFastExcel(DownloadRequest request) {
        log.info("🚀 FastExcel processing started: {}", request.getRequestId());
        
        String filePath = DOWNLOAD_DIR + request.getFileName();
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        
        try (OutputStream os = new FileOutputStream(filePath);
             Workbook workbook = new Workbook(os, "Excel Export", "1.0")) {
            
            Worksheet worksheet = workbook.newWorksheet("Data");
            
            // 헤더 설정
            worksheet.value(0, 0, "ID");
            worksheet.value(0, 1, "이름");
            worksheet.value(0, 2, "설명");
            worksheet.value(0, 3, "가격");
            worksheet.value(0, 4, "카테고리");
            worksheet.value(0, 5, "생성일시");
            
            // 수정: AtomicInteger 사용 (Excel 행 인덱스용)
            AtomicInteger currentRow = new AtomicInteger(1); // 헤더 다음부터
            AtomicLong processedCount = new AtomicLong(0);
            
            String sql = "SELECT id, name, description, price, category, created_at FROM sample_data ORDER BY id";
            
            jdbcTemplate.query(sql, rs -> {
                int rowIndex = currentRow.getAndIncrement();
                
                try {
                    // FastExcel 데이터 입력
                    worksheet.value(rowIndex, 0, rs.getLong("id"));
                    worksheet.value(rowIndex, 1, rs.getString("name"));
                    worksheet.value(rowIndex, 2, rs.getString("description"));
                    worksheet.value(rowIndex, 3, rs.getBigDecimal("price").doubleValue());
                    worksheet.value(rowIndex, 4, rs.getString("category"));
                    worksheet.value(rowIndex, 5, rs.getTimestamp("created_at").toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    
                    long processed = processedCount.incrementAndGet();
                    
                    // 진행률 업데이트 (10,000건마다)
                    if (processed % 10000 == 0) {
                        DownloadProgress progress = DownloadProgress.processing(
                                request.getRequestId(), -1, processed);
                        progressWebSocketHandler.sendProgress(request.getSessionId(), progress);
                        log.debug("FastExcel processed: {} rows", processed);
                    }
                } catch (Exception e) {
                    log.error("Error writing row {}: {}", rowIndex, e.getMessage());
                    throw new RuntimeException("Excel 행 작성 실패: " + e.getMessage(), e);
                }
            });
            
            // FastExcel 파일 완성
            workbook.finish();
            
            // 완료 알림
            String downloadUrl = "/api/download/file/" + request.getFileName();
            DownloadProgress completedProgress = DownloadProgress.completed(request.getRequestId(), downloadUrl);
            progressWebSocketHandler.sendProgress(request.getSessionId(), completedProgress);
            
            log.info("✅ FastExcel completed: {} ({} rows)", filePath, processedCount.get());
            
        } catch (Exception e) {
            log.error("❌ FastExcel processing failed: {}", request.getRequestId(), e);
            DownloadProgress failedProgress = DownloadProgress.failed(request.getRequestId(), e.getMessage());
            progressWebSocketHandler.sendProgress(request.getSessionId(), failedProgress);
            throw new RuntimeException("FastExcel 처리 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 🔥 FastExcel + 청크 처리 (DB 부하 최소화)
     */
    public void processWithFastExcelChunked(DownloadRequest request) {
        log.info("🚀 FastExcel Chunked processing: {}", request.getRequestId());
        
        String filePath = DOWNLOAD_DIR + request.getFileName();
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        
        try (OutputStream os = new FileOutputStream(filePath);
             Workbook workbook = new Workbook(os, "Excel Export", "1.0")) {
            
            Worksheet worksheet = workbook.newWorksheet("Data");
            
            // 헤더 설정
            worksheet.value(0, 0, "ID");
            worksheet.value(0, 1, "이름"); 
            worksheet.value(0, 2, "설명");
            worksheet.value(0, 3, "가격");
            worksheet.value(0, 4, "카테고리");
            worksheet.value(0, 5, "생성일시");
            
            // 전체 건수 조회
            Long totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample_data", Long.class);
            if (totalCount == null) totalCount = 0L;
            
            final int CHUNK_SIZE = 10000; // 1만건씩 청크 처리
            AtomicInteger currentExcelRow = new AtomicInteger(1); // ✅ 수정: AtomicInteger 사용
            long processedCount = 0;
            
            // 청크별 처리
            for (int offset = 0; offset < totalCount; offset += CHUNK_SIZE) {
                String chunkSql = "SELECT id, name, description, price, category, created_at " +
                                 "FROM sample_data ORDER BY id LIMIT ? OFFSET ?";
                
                // ✅ 수정: 각 청크마다 행 번호 추적
                jdbcTemplate.query(chunkSql, new Object[]{CHUNK_SIZE, offset}, rs -> {
                    try {
                        int rowIndex = currentExcelRow.getAndIncrement();
                        
                        worksheet.value(rowIndex, 0, rs.getLong("id"));
                        worksheet.value(rowIndex, 1, rs.getString("name"));
                        worksheet.value(rowIndex, 2, rs.getString("description"));
                        worksheet.value(rowIndex, 3, rs.getBigDecimal("price").doubleValue());
                        worksheet.value(rowIndex, 4, rs.getString("category"));
                        worksheet.value(rowIndex, 5, rs.getTimestamp("created_at").toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                                
                    } catch (Exception e) {
                        log.error("Error writing chunk row: {}", e.getMessage());
                        throw new RuntimeException("청크 행 작성 실패: " + e.getMessage(), e);
                    }
                });
                
                processedCount += Math.min(CHUNK_SIZE, totalCount - offset);
                
                // 진행률 업데이트 (청크마다)
                DownloadProgress progress = DownloadProgress.processing(
                        request.getRequestId(), totalCount, processedCount);
                progressWebSocketHandler.sendProgress(request.getSessionId(), progress);
                
                log.debug("FastExcel chunk processed: {}/{}", processedCount, totalCount);
            }
            
            workbook.finish();
            
            // 완료 알림
            String downloadUrl = "/api/download/file/" + request.getFileName();
            DownloadProgress completedProgress = DownloadProgress.completed(request.getRequestId(), downloadUrl);
            progressWebSocketHandler.sendProgress(request.getSessionId(), completedProgress);
            
            log.info("✅ FastExcel chunked completed: {} ({} rows)", filePath, processedCount);
            
        } catch (Exception e) {
            log.error("❌ FastExcel chunked failed: {}", request.getRequestId(), e);
            DownloadProgress failedProgress = DownloadProgress.failed(request.getRequestId(), e.getMessage());
            progressWebSocketHandler.sendProgress(request.getSessionId(), failedProgress);
            throw new RuntimeException("FastExcel 청크 처리 실패: " + e.getMessage(), e);
        }
    }
}
