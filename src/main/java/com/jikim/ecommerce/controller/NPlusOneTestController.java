package com.jikim.ecommerce.controller;

import com.jikim.ecommerce.service.NPlusOneTestService;
import com.jikim.ecommerce.service.EntityGraphTestService;
import com.jikim.ecommerce.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nplus1-test")
@RequiredArgsConstructor
@Slf4j
public class NPlusOneTestController {
    
    private final NPlusOneTestService nPlusOneTestService;
    private final EntityGraphTestService entityGraphTestService;
    
    // ===============================================
    // 생성(Creation) 테스트 메서드들
    // ===============================================
    
    /**
     * 1단계: N+1 문제 재현 (현재 상태)
     */
    @PostMapping("/problem")
    public ResponseEntity<Map<String, Object>> testNPlusOneProblem(
            @RequestParam(defaultValue = "10") int count) {
        return executeCreationTest(
            () -> nPlusOneTestService.createOrdersWithNPlusOneProblem(count),
            "🔥 N+1 문제 테스트",
            "N+1 문제 테스트 완료",
            count
        );
    }
    
    /**
     * 2단계: LAZY 로딩 테스트
     */
    @PostMapping("/lazy-loading")
    public ResponseEntity<Map<String, Object>> testLazyLoading(
            @RequestParam(defaultValue = "10") int count) {
        return executeCreationTest(
            () -> nPlusOneTestService.createOrdersWithLazyLoading(count),
            "🛠️ LAZY 로딩 테스트",
            "LAZY 로딩 테스트 완료",
            count
        );
    }
    
    /**
     * 4단계: 배치 조회 테스트
     */
    @PostMapping("/batch-fetch")
    public ResponseEntity<Map<String, Object>> testBatchFetch(
            @RequestParam(defaultValue = "10") int count) {
        return executeCreationTest(
            () -> nPlusOneTestService.createOrdersWithBatchFetch(count),
            "📦 배치 조회 테스트",
            "배치 조회 테스트 완료",
            count
        );
    }
    
    // ===============================================
    // 조회(Retrieval) 테스트 메서드들
    // ===============================================
    
    /**
     * 기본 조회 - N+1 문제 재현
     */
    @GetMapping("/retrieve-problem")
    public ResponseEntity<Map<String, Object>> testRetrievalProblem() {
        return executeRetrievalTest(
            () -> nPlusOneTestService.testOrderRetrievalWithNPlusOne(10),
            "🔍 주문 조회 N+1 문제 테스트",
            "주문 조회 N+1 문제 테스트 완료"
        );
    }
    
    /**
     * 3단계: @EntityGraph 조회 테스트
     */
    @GetMapping("/entity-graph")
    public ResponseEntity<Map<String, Object>> testEntityGraph() {
        return executeRetrievalTest(
            () -> entityGraphTestService.testEntityGraphRetrieval(),
            "🎯 @EntityGraph 조회 테스트",
            "@EntityGraph 조회 테스트 완료"
        );
    }
    
    /**
     * 5단계: JOIN FETCH 조회 테스트
     */
    @GetMapping("/join-fetch")
    public ResponseEntity<Map<String, Object>> testJoinFetch() {
        return executeRetrievalTest(
            () -> entityGraphTestService.testJoinFetchRetrieval(),
            "⚡ JOIN FETCH 조회 테스트",
            "JOIN FETCH 조회 테스트 완료"
        );
    }
    
    /**
     * 5-2단계: 간단한 JOIN FETCH 조회 테스트
     */
    @GetMapping("/simple-join-fetch")
    public ResponseEntity<Map<String, Object>> testSimpleJoinFetch() {
        return executeRetrievalTest(
            () -> entityGraphTestService.testSimpleJoinFetchRetrieval(),
            "⚡ 간단한 JOIN FETCH 조회 테스트",
            "간단한 JOIN FETCH 조회 테스트 완료"
        );
    }
    
    // ===============================================
    // 비교(Comparison) 테스트 메서드들
    // ===============================================
    
    /**
     * 조회 방법별 성능 비교
     */
    @GetMapping("/compare-retrieval")
    public ResponseEntity<Map<String, Object>> compareRetrievalMethods() {
        try {
            log.info("🏁 조회 방법별 성능 비교 테스트 시작");
            
            entityGraphTestService.compareRetrievalMethods();
            
            return ResponseEntity.ok(Map.of(
                "message", "조회 방법별 성능 비교 완료",
                "note", "로그에서 상세한 비교 결과를 확인해주세요"
            ));
            
        } catch (Exception e) {
            log.error("조회 성능 비교 테스트 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 생성 방법별 전체 성능 비교
     */
    @PostMapping("/compare-all")
    public ResponseEntity<Map<String, Object>> compareAllMethods(
            @RequestParam(defaultValue = "10") int count) {
        try {
            log.info("🏁 전체 성능 비교 테스트 시작 - 주문 {}개씩", count);
            
            // 1. N+1 문제 (기준)
            PerformanceMonitor.PerformanceMetrics problemMetrics = 
                nPlusOneTestService.createOrdersWithNPlusOneProblem(count);
            
            Thread.sleep(1000);
            
            // 2. LAZY 로딩
            PerformanceMonitor.PerformanceMetrics lazyMetrics = 
                nPlusOneTestService.createOrdersWithLazyLoading(count);
            
            Thread.sleep(1000);
            
            // 3. 배치 조회
            PerformanceMonitor.PerformanceMetrics batchMetrics = 
                nPlusOneTestService.createOrdersWithBatchFetch(count);
            
            // 결과 비교
            log.info("🎯 === 성능 비교 결과 ===");
            lazyMetrics.compareWith(problemMetrics);
            batchMetrics.compareWith(problemMetrics);
            
            return ResponseEntity.ok(Map.of(
                "message", "전체 성능 비교 완료",
                "orderCount", count,
                "results", Map.of(
                    "nPlusOneProblem", createMetricsMap(problemMetrics),
                    "lazyLoading", createMetricsMap(lazyMetrics),
                    "batchFetch", createMetricsMap(batchMetrics)
                )
            ));
            
        } catch (Exception e) {
            log.error("전체 성능 비교 테스트 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // ===============================================
    // Private Helper Methods (중복 제거)
    // ===============================================
    
    /**
     * 생성 테스트 실행 헬퍼 메서드
     */
    private ResponseEntity<Map<String, Object>> executeCreationTest(
            TestExecutor executor, String startMsg, String successMsg, int count) {
        try {
            log.info("{} 시작 - 주문 {}개", startMsg, count);
            
            PerformanceMonitor.PerformanceMetrics metrics = executor.execute();
            
            return ResponseEntity.ok(Map.of(
                "message", successMsg,
                "operation", metrics.getOperation(),
                "executionTimeMs", metrics.getExecutionTimeMs(),
                "queryCount", metrics.getQueryCount(),
                "orderCount", count
            ));
            
        } catch (Exception e) {
            log.error("{} 실패", startMsg, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 조회 테스트 실행 헬퍼 메서드
     */
    private ResponseEntity<Map<String, Object>> executeRetrievalTest(
            TestExecutor executor, String startMsg, String successMsg) {
        try {
            log.info("{} 시작", startMsg);
            
            PerformanceMonitor.PerformanceMetrics metrics = executor.execute();
            
            return ResponseEntity.ok(Map.of(
                "message", successMsg,
                "operation", metrics.getOperation(),
                "executionTimeMs", metrics.getExecutionTimeMs(),
                "queryCount", metrics.getQueryCount()
            ));
            
        } catch (Exception e) {
            log.error("{} 실패", startMsg, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 메트릭을 Map으로 변환
     */
    private Map<String, Object> createMetricsMap(PerformanceMonitor.PerformanceMetrics metrics) {
        return Map.of(
            "timeMs", metrics.getExecutionTimeMs(),
            "queries", metrics.getQueryCount(),
            "operation", metrics.getOperation()
        );
    }
    
    /**
     * 테스트 실행을 위한 함수형 인터페이스
     */
    @FunctionalInterface
    private interface TestExecutor {
        PerformanceMonitor.PerformanceMetrics execute() throws Exception;
    }
}
