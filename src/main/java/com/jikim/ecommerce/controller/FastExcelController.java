package com.jikim.ecommerce.controller;

import com.jikim.ecommerce.dto.DownloadRequest;
import com.jikim.ecommerce.service.FastExcelDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/fastexcel")
@RequiredArgsConstructor
@Slf4j
public class FastExcelController {
    
    private final FastExcelDownloadService fastExcelService;
    
    /**
     * 🔥 FastExcel 단일 쿼리 방식 테스트
     */
    @PostMapping("/single")
    public ResponseEntity<?> testFastExcelSingle(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.getOrDefault("sessionId", "test-session");
            String requestId = UUID.randomUUID().toString();
            String fileName = "fastexcel_single_" + requestId + ".xlsx";
            
            log.info("=== FastExcel Single Test ===");
            log.info("SessionId: {}", sessionId);
            log.info("RequestId: {}", requestId);
            
            DownloadRequest downloadRequest = DownloadRequest.builder()
                    .requestId(requestId)
                    .fileName(fileName)
                    .sessionId(sessionId)
                    .downloadType(DownloadRequest.DownloadType.STREAMING)
                    .build();
            
            // 🔥 비동기로 실행 (사용자는 즉시 응답 받음)
            CompletableFuture.runAsync(() -> {
                fastExcelService.processWithFastExcel(downloadRequest);
            });
            
            return ResponseEntity.ok(Map.of(
                    "requestId", requestId,
                    "fileName", fileName,
                    "message", "FastExcel 처리가 시작되었습니다",
                    "websocketUrl", "ws://localhost:8080/ws/download-progress?sessionId=" + sessionId
            ));
            
        } catch (Exception e) {
            log.error("FastExcel single test failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "TEST_FAILED",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 🔥 FastExcel 청크 방식 테스트
     */
    @PostMapping("/chunked")
    public ResponseEntity<?> testFastExcelChunked(@RequestBody Map<String, String> request) {
        try {
            String sessionId = request.getOrDefault("sessionId", "test-session");
            String requestId = UUID.randomUUID().toString();
            String fileName = "fastexcel_chunked_" + requestId + ".xlsx";
            
            log.info("=== FastExcel Chunked Test ===");
            log.info("SessionId: {}", sessionId);
            log.info("RequestId: {}", requestId);
            
            DownloadRequest downloadRequest = DownloadRequest.builder()
                    .requestId(requestId)
                    .fileName(fileName)
                    .sessionId(sessionId)
                    .downloadType(DownloadRequest.DownloadType.STREAMING)
                    .build();
            
            // 🔥 비동기로 실행
            CompletableFuture.runAsync(() -> {
                fastExcelService.processWithFastExcelChunked(downloadRequest);
            });
            
            return ResponseEntity.ok(Map.of(
                    "requestId", requestId,
                    "fileName", fileName,
                    "message", "FastExcel 청크 처리가 시작되었습니다",
                    "websocketUrl", "ws://localhost:8080/ws/download-progress?sessionId=" + sessionId
            ));
            
        } catch (Exception e) {
            log.error("FastExcel chunked test failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "TEST_FAILED",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 🔥 동시성 테스트용 - 3개 동시 요청
     */
    @PostMapping("/concurrent-test")
    public ResponseEntity<?> testConcurrent() {
        try {
            log.info("=== 3개 동시 FastExcel 테스트 시작 ===");
            
            for (int i = 1; i <= 3; i++) {
                String sessionId = "concurrent-test-" + i;
                String requestId = UUID.randomUUID().toString();
                String fileName = "fastexcel_concurrent_" + i + "_" + requestId + ".xlsx";
                
                DownloadRequest downloadRequest = DownloadRequest.builder()
                        .requestId(requestId)
                        .fileName(fileName)
                        .sessionId(sessionId)
                        .downloadType(DownloadRequest.DownloadType.STREAMING)
                        .build();
                
                // 🔥 3개 모두 비동기로 시작
                CompletableFuture.runAsync(() -> {
                    fastExcelService.processWithFastExcelChunked(downloadRequest);
                });
                
                log.info("동시 테스트 {}번 시작: {}", i, requestId);
            }
            
            return ResponseEntity.ok(Map.of(
                    "message", "3개 동시 FastExcel 처리 시작",
                    "note", "각 세션별로 WebSocket 연결해서 진행률 확인하세요"
            ));
            
        } catch (Exception e) {
            log.error("Concurrent test failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "CONCURRENT_TEST_FAILED",
                    "message", e.getMessage()
            ));
        }
    }
}
