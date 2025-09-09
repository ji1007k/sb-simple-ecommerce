package com.jikim.ecommerce.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class PerformanceMonitor {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final AtomicInteger queryCounter = new AtomicInteger(0);
    
    /**
     * 성능 측정 시작
     */
    public PerformanceResult startMeasurement(String operation) {
        return new PerformanceResult(operation);
    }
    
    /**
     * 쿼리 카운터 리셋
     */
    public void resetQueryCounter() {
        queryCounter.set(0);
    }
    
    /**
     * 쿼리 카운터 증가
     */
    public void incrementQueryCounter() {
        queryCounter.incrementAndGet();
    }
    
    /**
     * 현재 쿼리 카운트 조회
     */
    public int getCurrentQueryCount() {
        return queryCounter.get();
    }
    
    /**
     * 성능 측정 결과 클래스
     */
    public static class PerformanceResult {
        private final String operation;
        private final StopWatch stopWatch;
        private final LocalDateTime startTime;
        private int startQueryCount;
        private int endQueryCount;
        
        public PerformanceResult(String operation) {
            this.operation = operation;
            this.stopWatch = new StopWatch(operation);
            this.startTime = LocalDateTime.now();
            this.stopWatch.start();
            
            log.info("🚀 [{}] 시작 - {}", operation, startTime.format(TIME_FORMATTER));
        }
        
        public void setStartQueryCount(int count) {
            this.startQueryCount = count;
        }
        
        /**
         * 측정 완료 및 결과 출력
         */
        public PerformanceMetrics finish(int endQueryCount) {
            this.endQueryCount = endQueryCount;
            stopWatch.stop();
            
            LocalDateTime endTime = LocalDateTime.now();
            long executionTimeMs = stopWatch.getTotalTimeMillis();
            int totalQueries = endQueryCount - startQueryCount;
            
            log.info("🏁 [{}] 완료 - {}", operation, endTime.format(TIME_FORMATTER));
            log.info("📊 [{}] 성능 측정 결과:", operation);
            log.info("   ⏱️  실행 시간: {}ms", executionTimeMs);
            log.info("   🔍 실행 쿼리 수: {}번", totalQueries);
            if (totalQueries > 0) {
                double avgTime = (double) executionTimeMs / totalQueries;
                log.info("   📈 쿼리당 평균 시간: {}ms", String.format("%.2f", avgTime));
            } else {
                log.info("   📈 쿼리당 평균 시간: N/A (쿼리 없음)");
            }
            log.info("────────────────────────────────────────");
            
            return new PerformanceMetrics(operation, executionTimeMs, totalQueries);
        }
        
        public PerformanceMetrics finish() {
            return finish(0);
        }
    }
    
    /**
     * 성능 측정 결과 데이터
     */
    public static class PerformanceMetrics {
        private final String operation;
        private final long executionTimeMs;
        private final int queryCount;
        
        public PerformanceMetrics(String operation, long executionTimeMs, int queryCount) {
            this.operation = operation;
            this.executionTimeMs = executionTimeMs;
            this.queryCount = queryCount;
        }
        
        public String getOperation() { return operation; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public int getQueryCount() { return queryCount; }
        
        /**
         * 다른 메트릭과 비교 (현재 메트릭이 개선된 결과)
         */
        public void compareWith(PerformanceMetrics baseline) {
            log.info("🔄 성능 비교: {} → {} (개선)", baseline.operation, this.operation);
            
            log.info("   실행 시간: {}ms → {}ms ({}%)", 
                baseline.executionTimeMs, 
                this.executionTimeMs,
                calculatePercentageChange(baseline.executionTimeMs, this.executionTimeMs));
                
            log.info("   쿼리 수: {}번 → {}번 ({}%)",
                baseline.queryCount,
                this.queryCount,
                calculatePercentageChange(baseline.queryCount, this.queryCount));
        }
        
        private String calculatePercentageChange(long oldValue, long newValue) {
            if (oldValue == 0) return "N/A";
            double change = ((double) (newValue - oldValue) / oldValue) * 100;
            return String.format("%+.1f", change);
        }
    }
}
