package com.jikim.ecommerce.config;

import com.jikim.ecommerce.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Hibernate 쿼리 실행을 감지하여 카운팅하는 인터셉터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueryCounterInterceptor implements Interceptor {
    
    private final PerformanceMonitor performanceMonitor;
    
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        performanceMonitor.incrementQueryCounter();
        log.debug("🔍 Query executed - Load: {}", entity.getClass().getSimpleName());
        return false;
    }
    
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        performanceMonitor.incrementQueryCounter();
        log.debug("🔍 Query executed - Save: {}", entity.getClass().getSimpleName());
        return false;
    }
    
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        performanceMonitor.incrementQueryCounter();
        log.debug("🔍 Query executed - Delete: {}", entity.getClass().getSimpleName());
    }
    
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        performanceMonitor.incrementQueryCounter();
        log.debug("🔍 Query executed - Update: {}", entity.getClass().getSimpleName());
        return false;
    }
}
