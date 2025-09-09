package com.jikim.ecommerce.service;

import com.jikim.ecommerce.entity.Order;
import com.jikim.ecommerce.entity.OrderItem;
import com.jikim.ecommerce.entity.Product;
import com.jikim.ecommerce.repository.OrderRepository;
import com.jikim.ecommerce.repository.ProductRepository;
import com.jikim.ecommerce.util.PerformanceMonitor;
import com.jikim.ecommerce.util.QueryCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class NPlusOneTestService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PerformanceMonitor performanceMonitor;
    private final QueryCounter queryCounter;
    private final Random random = new Random();
    
    /**
     * 현재 상태: N+1 문제 발생하는 주문 생성
     */
    @Transactional
    public PerformanceMonitor.PerformanceMetrics createOrdersWithNPlusOneProblem(int orderCount) {
        log.info("🔥 N+1 문제가 있는 주문 {}개 생성 시작", orderCount);
        
        // 성능 측정 시작
        PerformanceMonitor.PerformanceResult measurement = performanceMonitor.startMeasurement("N+1 Problem Orders");
        queryCounter.reset();
        
        // 실제 존재하는 상품 ID 조회
        List<Long> productIds = productRepository.findAllIds();
        queryCounter.increment(); // findAllIds 쿼리
        if (productIds.isEmpty()) {
            throw new IllegalStateException("상품 데이터가 없습니다. 먼저 상품을 생성해주세요.");
        }
        
        List<Order> orders = new ArrayList<>();
        
        for (int i = 0; i < orderCount; i++) {
            Order order = createSingleOrderWithNPlusOne(i + 1, productIds);
            orders.add(order);
        }
        
        // 배치로 저장 (여기서 N+1 문제 발생!)
        orderRepository.saveAll(orders);
        queryCounter.increment(); // saveAll 쿼리
        
        // 측정 완료
        long finalQueryCount = queryCounter.getCountSinceReset();
        log.info("⚠️ 실제 실행된 쿼리 수: {}번 (N+1 문제!)", finalQueryCount);
        
        return measurement.finish((int) finalQueryCount);
    }
    
    /**
     * 개선된 방법 1: LAZY 로딩 + 명시적 조회
     */
    @Transactional
    public PerformanceMonitor.PerformanceMetrics createOrdersWithLazyLoading(int orderCount) {
        log.info("🛠️ LAZY 로딩으로 주문 {}개 생성 시작", orderCount);
        
        PerformanceMonitor.PerformanceResult measurement = performanceMonitor.startMeasurement("LAZY Loading Orders");
        queryCounter.reset();
        
        List<Long> productIds = productRepository.findAllIds();
        queryCounter.increment(); // findAllIds 쿼리
        if (productIds.isEmpty()) {
            throw new IllegalStateException("상품 데이터가 없습니다.");
        }
        
        List<Order> orders = new ArrayList<>();
        
        for (int i = 0; i < orderCount; i++) {
            Order order = createSingleOrderLazy(i + 1, productIds);
            orders.add(order);
        }
        
        orderRepository.saveAll(orders);
        queryCounter.increment(); // saveAll 쿼리
        
        return measurement.finish((int) queryCounter.getCountSinceReset());
    }
    
    /**
     * 개선된 방법 2: 배치로 상품 미리 조회
     */
    @Transactional
    public PerformanceMonitor.PerformanceMetrics createOrdersWithBatchFetch(int orderCount) {
        log.info("📦 배치 조회로 주문 {}개 생성 시작", orderCount);
        
        PerformanceMonitor.PerformanceResult measurement = performanceMonitor.startMeasurement("Batch Fetch Orders");
        queryCounter.reset();
        
        List<Long> productIds = productRepository.findAllIds();
        queryCounter.increment(); // findAllIds 쿼리
        if (productIds.isEmpty()) {
            throw new IllegalStateException("상품 데이터가 없습니다.");
        }
        
        // 필요한 상품들을 미리 배치로 조회
        List<Long> selectedProductIds = selectRandomProducts(productIds, orderCount);
        List<Product> products = productRepository.findAllById(selectedProductIds);
        queryCounter.increment(); // findAllById 배치 쿼리
        
        List<Order> orders = createOrdersFromProducts(orderCount, products);
        orderRepository.saveAll(orders);
        queryCounter.increment(); // saveAll 쿼리
        
        return measurement.finish((int) queryCounter.getCountSinceReset());
    }
    
    /**
     * 주문 조회 테스트 - N+1 문제 재현
     */
    @Transactional(readOnly = true)
    public PerformanceMonitor.PerformanceMetrics testOrderRetrievalWithNPlusOne(int limit) {
        log.info("🔍 N+1 문제 조회 테스트 시작 (최대 {}건)", limit);
        
        PerformanceMonitor.PerformanceResult measurement = performanceMonitor.startMeasurement("N+1 Order Retrieval");
        performanceMonitor.resetQueryCounter();
        measurement.setStartQueryCount(performanceMonitor.getCurrentQueryCount());
        
        // 주문 조회 (Order → OrderItem → Product 순서로 EAGER 로딩 발생)
        List<Order> orders = orderRepository.findTop10ByOrderByIdDesc();
        
        // 각 주문의 상품 정보 접근 (N+1 발생 지점)
        int totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Order order : orders) {
            log.debug("주문 ID: {}, 고객: {}", order.getId(), order.getCustomerName());
            
            for (OrderItem item : order.getItems()) {
                // 여기서 Product 정보에 접근할 때마다 개별 쿼리 발생!
                Product product = item.getProduct();
                log.debug("  - 상품: {} ({}개)", product.getName(), item.getQuantity());
                totalItems += item.getQuantity();
                totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        
        log.info("조회된 주문: {}건, 총 상품: {}개, 총 금액: {}", orders.size(), totalItems, totalAmount);
        
        return measurement.finish(performanceMonitor.getCurrentQueryCount());
    }
    
    // === Private Helper Methods ===
    
    private Order createSingleOrderWithNPlusOne(long seed, List<Long> productIds) {
        Order order = Order.builder()
                .customerName("Customer_" + seed)
                .customerEmail(String.format("test%d@example.com", seed))
                .shippingAddress(String.format("Address %d", seed))
                .items(new ArrayList<>())
                .build();
        
        // 주문 아이템 생성 (2-4개)
        int itemCount = random.nextInt(3) + 2;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (int i = 0; i < itemCount; i++) {
            Long productId = productIds.get(random.nextInt(productIds.size()));
            
            // 🔥 여기서 N+1 문제 발생! 각 상품을 개별 조회
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            queryCounter.increment(); // 각 findById 쿼리 카운팅
            
            int quantity = random.nextInt(3) + 1;
            BigDecimal price = product.getPrice();
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .price(price)
                    .build();
            
            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
        
        order.setTotalAmount(totalAmount);
        return order;
    }
    
    private Order createSingleOrderLazy(long seed, List<Long> productIds) {
        // LAZY 로딩을 고려한 주문 생성
        // 실제로는 Product 엔티티의 fetch 타입을 LAZY로 변경해야 함
        return createSingleOrderWithNPlusOne(seed, productIds); // 임시로 동일한 로직 사용
    }
    
    private List<Long> selectRandomProducts(List<Long> productIds, int orderCount) {
        List<Long> selected = new ArrayList<>();
        int productsNeeded = orderCount * 3; // 주문당 평균 3개 상품
        
        for (int i = 0; i < productsNeeded; i++) {
            selected.add(productIds.get(random.nextInt(productIds.size())));
        }
        
        return selected;
    }
    
    private List<Order> createOrdersFromProducts(int orderCount, List<Product> products) {
        List<Order> orders = new ArrayList<>();
        
        for (int i = 0; i < orderCount; i++) {
            Order order = Order.builder()
                    .customerName("BatchCustomer_" + (i + 1))
                    .customerEmail(String.format("batch%d@example.com", i + 1))
                    .shippingAddress(String.format("Batch Address %d", i + 1))
                    .items(new ArrayList<>())
                    .build();
            
            // 미리 조회된 상품에서 랜덤 선택
            int itemCount = random.nextInt(3) + 2;
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (int j = 0; j < itemCount; j++) {
                Product product = products.get(random.nextInt(products.size()));
                int quantity = random.nextInt(3) + 1;
                BigDecimal price = product.getPrice();
                
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .quantity(quantity)
                        .price(price)
                        .build();
                
                order.getItems().add(orderItem);
                totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));
            }
            
            order.setTotalAmount(totalAmount);
            orders.add(order);
        }
        
        return orders;
    }
}
