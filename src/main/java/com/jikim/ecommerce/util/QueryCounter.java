package com.jikim.ecommerce.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 간단한 쿼리 카운터 (수동 카운팅)
 */
@Component
@Slf4j
public class QueryCounter {
    
    private final AtomicLong queryCount = new AtomicLong(0);
    private final AtomicLong startCount = new AtomicLong(0);
    
    /**
     * 카운터 초기화
     */
    public void reset() {
        startCount.set(queryCount.get());
        log.debug("🔄 Query counter reset. Current total: {}", queryCount.get());
    }
    
    /**
     * 쿼리 카운트 증가
     */
    public void increment() {
        long current = queryCount.incrementAndGet();
        log.debug("🔍 Query executed. Total count: {}", current);
    }
    
    /**
     * 시작점 이후 실행된 쿼리 수
     */
    public long getCountSinceReset() {
        return queryCount.get() - startCount.get();
    }
    
    /**
     * 전체 쿼리 수
     */
    public long getTotalCount() {
        return queryCount.get();
    }
    
    /**
     * 수동으로 쿼리 수 설정 (테스트용)
     */
    public void setCount(long count) {
        queryCount.set(count);
        log.debug("🎯 Query count manually set to: {}", count);
    }
}
