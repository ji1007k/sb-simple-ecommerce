package com.jikim.ecommerce.service;

import com.jikim.ecommerce.entity.Order;
import com.jikim.ecommerce.entity.OrderItem;
import com.jikim.ecommerce.entity.Product;
import com.jikim.ecommerce.repository.OrderRepository;
import com.jikim.ecommerce.util.PerformanceMonitor;
import com.jikim.ecommerce.util.QueryCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntityGraphTestService {
    
    private final OrderRepository orderRepository;
    private final PerformanceMonitor performanceMonitor;
    private final QueryCounter queryCounter;
    
    // ===============================================
    // 개별 조회 테스트 메서드들
    // ===============================================
    
    /**
     * 기본 조회 (비교 기준) - N+1 문제 발생
     */
    @Transactional(readOnly = true)
    public PerformanceMonitor.PerformanceMetrics testBasicRetrieval() {
        return executeRetrievalTest(
            "📊 기본 조회 테스트",
            "Basic Retrieval", 
            () -> {
                List<Order> orders = orderRepository.findTop10ByOrderByIdDesc();
                queryCounter.increment(); // 기본 조회 쿼리
                return orders;
            },
            true // LAZY 로딩으로 인한 추가 쿼리 발생
        );
    }
    
    /**
     * 3단계: @EntityGraph를 사용한 최적화 조회
     */
    @Transactional(readOnly = true)
    public PerformanceMonitor.PerformanceMetrics testEntityGraphRetrieval() {
        return executeRetrievalTest(
            "🎯 @EntityGraph 최적화 조회 테스트",
            "EntityGraph Optimized Retrieval",
            () -> {
                List<Order> orders = orderRepository.findTop10WithItemsAndProducts();
                queryCounter.increment(); // EntityGraph 쿼리
                return orders;
            },
            false // 이미 EntityGraph로 로딩되어 추가 쿼리 없음
        );
    }
    
    /**
     * 5-1단계: 간단한 JOIN FETCH 테스트
     */
    @Transactional(readOnly = true)
    public PerformanceMonitor.PerformanceMetrics testSimpleJoinFetchRetrieval() {
        return executeRetrievalTest(
            "⚡ 간단한 JOIN FETCH 조회 테스트",
            "Simple JOIN FETCH Retrieval",
            () -> {
                List<Order> orders = orderRepository.findTop10WithSimpleJoinFetch();
                queryCounter.increment(); // 단일 JOIN FETCH 쿼리
                return orders;
            },
            false // JOIN FETCH로 모든 데이터 로딩됨
        );
    }
    
    /**
     * 5-2단계: 2단계 JOIN FETCH 테스트 (개선된 방법)
     */
    @Transactional(readOnly = true)
    public PerformanceMonitor.PerformanceMetrics testJoinFetchRetrieval() {
        log.info("⚡ 2단계 JOIN FETCH 최적화 조회 테스트 시작");
        
        PerformanceMonitor.PerformanceResult measurement = performanceMonitor.startMeasurement("Two-Step JOIN FETCH Retrieval");
        queryCounter.reset();
        
        // 1단계: 먼저 Top 10 주문 ID만 조회
        List<Order> topOrders = orderRepository.findTop10OrderIds();
        queryCounter.increment(); // ID 조회 쿼리
        
        List<Long> orderIds = topOrders.stream().map(Order::getId).toList();
        
        // 2단계: JOIN FETCH로 상세 정보 조회
        List<Order> orders = orderRepository.findTop10WithJoinFetch(orderIds);
        queryCounter.increment(); // JOIN FETCH 쿼리
        
        // 조회된 데이터 처리
        OrderProcessingResult result = processOrderData(orders, false);
        
        log.info("2단계 JOIN FETCH 조회 결과: 주문 {}건, 총 상품 {}개, 총 금액 {}", 
                orders.size(), result.totalItems(), result.totalAmount());
        
        return measurement.finish((int) queryCounter.getCountSinceReset());
    }
    
    // ===============================================
    // 비교 테스트 메서드
    // ===============================================
    
    /**
     * 모든 조회 방법 성능 비교
     */
    @Transactional(readOnly = true)
    public void compareRetrievalMethods() {
        log.info("🏁 조회 방법별 성능 비교 시작");
        
        // 1. 기본 조회 (N+1 문제 발생)
        PerformanceMonitor.PerformanceMetrics basicMetrics = testBasicRetrieval();
        sleepQuietly(500);
        
        // 2. EntityGraph 최적화 조회
        PerformanceMonitor.PerformanceMetrics entityGraphMetrics = testEntityGraphRetrieval();
        sleepQuietly(500);
        
        // 3. 간단한 JOIN FETCH 최적화 조회
        PerformanceMonitor.PerformanceMetrics simpleJoinFetchMetrics = testSimpleJoinFetchRetrieval();
        sleepQuietly(500);
        
        // 4. 2단계 JOIN FETCH 최적화 조회
        PerformanceMonitor.PerformanceMetrics twoStepJoinFetchMetrics = testJoinFetchRetrieval();
        
        // 결과 비교
        log.info("🎯 === 기본 조회 대비 최적화 성능 ===");
        entityGraphMetrics.compareWith(basicMetrics);
        simpleJoinFetchMetrics.compareWith(basicMetrics);
        twoStepJoinFetchMetrics.compareWith(basicMetrics);
        
        log.info("🔍 === 최적화 방법 간 성능 비교 ===");
        simpleJoinFetchMetrics.compareWith(entityGraphMetrics);
        twoStepJoinFetchMetrics.compareWith(entityGraphMetrics);
        twoStepJoinFetchMetrics.compareWith(simpleJoinFetchMetrics);
    }
    
    // ===============================================
    // Private Helper Methods (중복 제거)
    // ===============================================
    
    /**
     * 조회 테스트 실행 헬퍼 메서드
     */
    private PerformanceMonitor.PerformanceMetrics executeRetrievalTest(
            String startMsg, 
            String operationName, 
            Supplier<List<Order>> queryExecutor,
            boolean hasLazyLoading) {
        
        log.info("{} 시작", startMsg);
        
        PerformanceMonitor.PerformanceResult measurement = performanceMonitor.startMeasurement(operationName);
        queryCounter.reset();
        
        // 쿼리 실행
        List<Order> orders = queryExecutor.get();
        
        // 데이터 처리
        OrderProcessingResult result = processOrderData(orders, hasLazyLoading);
        
        log.info("{} 결과: 주문 {}건, 총 상품 {}개, 총 금액 {}", 
                operationName, orders.size(), result.totalItems(), result.totalAmount());
        
        return measurement.finish((int) queryCounter.getCountSinceReset());
    }
    
    /**
     * 주문 데이터 처리 (공통 로직)
     */
    private OrderProcessingResult processOrderData(List<Order> orders, boolean hasLazyLoading) {
        int totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Order order : orders) {
            log.debug("주문 ID: {}, 고객: {}", order.getId(), order.getCustomerName());
            
            for (OrderItem item : order.getItems()) {
                // LAZY 로딩 여부에 따라 쿼리 카운팅
                Product product = item.getProduct();
                if (hasLazyLoading) {
                    queryCounter.increment(); // 각 Product 지연 로딩 쿼리
                }
                
                log.debug("  - 상품: {} ({}개)", product.getName(), item.getQuantity());
                totalItems += item.getQuantity();
                totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        
        return new OrderProcessingResult(totalItems, totalAmount);
    }
    
    /**
     * 조용히 대기하는 헬퍼 메서드
     */
    private void sleepQuietly(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 주문 처리 결과를 담는 레코드
     */
    private record OrderProcessingResult(int totalItems, BigDecimal totalAmount) {}
}
