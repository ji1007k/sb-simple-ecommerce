package com.jikim.ecommerce.controller;

import com.jikim.ecommerce.dto.DownloadRequest;
import com.jikim.ecommerce.service.ExcelDownloadQueue;
import com.jikim.ecommerce.service.ExcelDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
@Slf4j
public class ExcelDownloadController {
    
    private final ExcelDownloadService excelDownloadService;
    private static final String DOWNLOAD_DIR = "downloads/";
    
    /**
     * 엑셀 다운로드 요청 (기존 방식 - 페이징)
     */
    @PostMapping("/excel/paging")
    public ResponseEntity<Map<String, String>> requestExcelDownloadPaging(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        
        try {
            if (sessionId == null) {
                sessionId = "default-session";
            }

            String requestId = UUID.randomUUID().toString();
            String finalSessionId = sessionId;
            CompletableFuture.runAsync(() -> {
                excelDownloadService.requestDownload(
                        DownloadRequest.DownloadType.PAGING, finalSessionId, requestId);
            });

            return ResponseEntity.ok(Map.of(
                    "requestId", requestId,
                    "message", "다운로드 요청이 큐에 추가되었습니다. WebSocket으로 진행률을 확인하세요."
            ));
        } catch (Exception e) {
            log.error("Failed to request excel download (paging)", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 엑셀 다운로드 요청 (개선 방식 - 스트리밍)
     */
    @PostMapping("/excel/streaming")
    public ResponseEntity<Map<String, String>> requestExcelDownloadStreaming(
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        
        try {
            if (sessionId == null) {
                sessionId = "default-session";
            }

            String requestId = UUID.randomUUID().toString();
            String finalSessionId = sessionId;
            CompletableFuture.runAsync(() -> {
                excelDownloadService.requestDownload(
                        DownloadRequest.DownloadType.PAGING, finalSessionId, requestId);
            });
            
            return ResponseEntity.ok(Map.of(
                    "requestId", requestId,
                    "message", "다운로드 요청이 큐에 추가되었습니다. WebSocket으로 진행률을 확인하세요."
            ));
        } catch (Exception e) {
            log.error("Failed to request excel download (streaming)", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 완성된 파일 다운로드
     */
    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            File file = new File(DOWNLOAD_DIR + fileName);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 큐 상태 조회
     */
    @GetMapping("/queue/status")
    public ResponseEntity<ExcelDownloadQueue.QueueStatus> getQueueStatus() {
        try {
            ExcelDownloadQueue.QueueStatus status = excelDownloadService.getQueueStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get queue status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
