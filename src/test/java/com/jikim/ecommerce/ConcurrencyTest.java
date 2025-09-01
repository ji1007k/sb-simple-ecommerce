package com.jikim.ecommerce;

import com.jikim.ecommerce.dto.OrderRequest;
import com.jikim.ecommerce.entity.Product;
import com.jikim.ecommerce.repository.ProductRepository;
import com.jikim.ecommerce.service.CartService;
import com.jikim.ecommerce.service.OrderService;
import com.jikim.ecommerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConcurrencyTest {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ProductRepository productRepository;
    
    private Product testProduct;
    
    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성 (재고 100개)
        testProduct = Product.builder()
                .name("테스트 상품")
                .description("동시성 테스트용 상품")
                .price(new BigDecimal("10000"))
                .stock(100)
                .category("테스트")
                .imageUrl("test.jpg")
                .build();
        testProduct = productRepository.save(testProduct);
        
        System.out.println("\n===== 테스트 상품 생성 완료 =====");
        System.out.println("상품 ID: " + testProduct.getId());
        System.out.println("초기 재고: " + testProduct.getStock());
        System.out.println("==================================\n");
    }
    
    @Test
    @DisplayName("동시에 여러 주문이 들어와도 재고가 정확하게 관리되어야 한다")
    void testConcurrentOrders() throws InterruptedException {
        System.out.println("\n===== 동시성 주문 테스트 시작 =====");
        
        int numberOfThreads = 10;  // 동시 실행 스레드 수
        int ordersPerThread = 5;   // 각 스레드당 주문 수
        int quantityPerOrder = 2;  // 주문당 수량
        
        System.out.println("스레드 수: " + numberOfThreads);
        System.out.println("스레드당 주문 수: " + ordersPerThread);
        System.out.println("주문당 수량: " + quantityPerOrder);
        System.out.println("총 요청 수량: " + (numberOfThreads * ordersPerThread * quantityPerOrder));
        
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // 각 스레드에서 주문 실행
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        String sessionId = "thread-" + threadId + "-order-" + j;
                        
                        try {
                            // 장바구니에 상품 추가
                            cartService.addToCart(sessionId, testProduct.getId(), quantityPerOrder);
                            
                            // 주문 생성
                            OrderRequest orderRequest = new OrderRequest();
                            orderRequest.setCustomerName("Customer " + threadId + "-" + j);
                            orderRequest.setCustomerEmail("customer" + threadId + "_" + j + "@test.com");
                            orderRequest.setShippingAddress("Address " + threadId + "-" + j);
                            
                            orderService.createOrder(sessionId, orderRequest);
                            successCount.incrementAndGet();
                            
                            System.out.println("[성공] Thread-" + threadId + " Order-" + j + " 주문 완료");
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            exceptions.add(e);
                            System.out.println("[실패] Thread-" + threadId + " Order-" + j + " - " + e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 모든 스레드 완료 대기
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "테스트가 시간 내에 완료되지 않았습니다");
        
        long endTime = System.currentTimeMillis();
        executorService.shutdown();
        
        // 결과 검증
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        int totalRequestedQuantity = numberOfThreads * ordersPerThread * quantityPerOrder;
        int expectedRemainingStock = Math.max(0, 100 - totalRequestedQuantity);
        
        System.out.println("\n===== 테스트 결과 =====");
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("초기 재고: 100");
        System.out.println("요청된 총 수량: " + totalRequestedQuantity);
        System.out.println("성공한 주문 수: " + successCount.get());
        System.out.println("실패한 주문 수: " + failCount.get());
        System.out.println("최종 재고: " + finalProduct.getStock());
        System.out.println("예상 재고: " + expectedRemainingStock);
        System.out.println("======================\n");
        
        // 재고가 음수가 되지 않았는지 확인
        assertTrue(finalProduct.getStock() >= 0, "재고가 음수가 되어서는 안됩니다");
        
        // 성공한 주문 수와 재고 감소량이 일치하는지 확인
        int actualSoldQuantity = 100 - finalProduct.getStock();
        int expectedSoldQuantity = successCount.get() * quantityPerOrder;
        assertEquals(expectedSoldQuantity, actualSoldQuantity, 
                "판매된 수량이 성공한 주문의 합과 일치해야 합니다");
        
        // 재고 부족으로 일부 주문이 실패했는지 확인
        if (totalRequestedQuantity > 100) {
            assertTrue(failCount.get() > 0, "재고를 초과한 주문은 실패해야 합니다");
            System.out.println("✓ 재고 초과 주문이 정상적으로 실패했습니다.");
        }
        
        System.out.println("✓ 동시성 테스트 통과!");
    }
    
    @Test
    @DisplayName("낙관적 락이 제대로 동작하는지 테스트")
    void testOptimisticLocking() throws InterruptedException {
        System.out.println("\n===== 낙관적 락 테스트 시작 =====");
        
        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        
        System.out.println("동시 실행 스레드 수: " + numberOfThreads);
        System.out.println("각 스레드당 주문 수량: 10개");
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    // 모든 스레드가 동시에 시작하도록 대기
                    startLatch.await();
                    
                    String sessionId = "optimistic-test-" + threadId;
                    
                    // 장바구니에 추가
                    cartService.addToCart(sessionId, testProduct.getId(), 10);
                    
                    // 주문 생성 (retry 포함)
                    OrderRequest orderRequest = new OrderRequest();
                    orderRequest.setCustomerName("Optimistic Test " + threadId);
                    orderRequest.setCustomerEmail("optimistic" + threadId + "@test.com");
                    orderRequest.setShippingAddress("Test Address " + threadId);
                    
                    int attempts = 0;
                    boolean success = false;
                    Exception lastException = null;
                    
                    while (attempts < 3 && !success) {
                        attempts++;
                        try {
                            orderService.createOrder(sessionId, orderRequest);
                            success = true;
                            successCount.incrementAndGet();
                            System.out.println("[성공] Thread-" + threadId + " (시도 횟수: " + attempts + ")");
                        } catch (Exception e) {
                            lastException = e;
                            if (attempts > 1) {  // 2번째 시도부터 재시도로 카운트
                                System.out.println("[재시도] Thread-" + threadId + " 재시도 " + (attempts-1) + "회째: " + e.getMessage());
                            } else {
                                System.out.println("[첫시도 실패] Thread-" + threadId + ": " + e.getMessage());
                            }

                            retryCount.incrementAndGet();
                            
                            if (attempts < 3) {  // 마지막 시도가 아니면 대기
                                Thread.sleep(50);
                            }
                        }
                    }
                    
                    if (!success) {
                        System.out.println("[최종실패] Thread-" + threadId + ": " + lastException.getMessage());
                    }
                    
                } catch (Exception e) {
                    System.out.println("[에러] Thread-" + threadId + ": " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        // 모든 스레드 동시 시작
        System.out.println("\n모든 스레드 동시 시작!");
        startLatch.countDown();
        
        // 모든 스레드 완료 대기
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "테스트가 시간 내에 완료되지 않았습니다");
        
        executorService.shutdown();
        
        // 결과 확인
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        
        System.out.println("\n===== 낙관적 락 테스트 결과 =====");
        System.out.println("성공한 주문 수: " + successCount.get());
        System.out.println("재시도 횟수: " + retryCount.get());
        System.out.println("최종 재고: " + finalProduct.getStock());
        System.out.println("================================");
        
        // 재고 일관성 확인
        int expectedStock = 100 - (successCount.get() * 10);
        assertEquals(expectedStock, finalProduct.getStock(), 
                "재고가 정확하게 관리되어야 합니다");
        
        // 재시도가 발생했는지 확인 (동시성 충돌이 있었다는 증거)
        assertTrue(retryCount.get() > 0 || successCount.get() < numberOfThreads, 
                "동시성 충돌이 발생하여 재시도나 실패가 있어야 합니다");
        
        System.out.println("✓ 낙관적 락 테스트 통과!");
    }
    
    @Test
    @DisplayName("재고가 정확히 0이 될 때까지만 주문이 성공해야 한다")
    void testExactStockDepletion() throws InterruptedException {
        System.out.println("\n===== 정확한 재고 소진 테스트 시작 =====");
        
        // 재고를 딱 맞게 설정
        testProduct.setStock(10);
        productRepository.save(testProduct);
        
        System.out.println("초기 재고: 10개");
        
        int numberOfOrders = 15; // 재고보다 많은 주문
        System.out.println("주문 시도 수: " + numberOfOrders + "개 (재고보다 5개 많음)");
        
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfOrders);
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        for (int i = 0; i < numberOfOrders; i++) {
            final int orderId = i;
            executorService.submit(() -> {
                try {
                    String sessionId = "exact-test-" + orderId;
                    
                    // 각 주문당 1개씩
                    cartService.addToCart(sessionId, testProduct.getId(), 1);
                    
                    OrderRequest orderRequest = new OrderRequest();
                    orderRequest.setCustomerName("Exact Test " + orderId);
                    orderRequest.setCustomerEmail("exact" + orderId + "@test.com");
                    orderRequest.setShippingAddress("Address " + orderId);
                    
                    try {
                        orderService.createOrder(sessionId, orderRequest);
                        successCount.incrementAndGet();
                        System.out.println("[성공] 주문 #" + orderId);
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        System.out.println("[실패] 주문 #" + orderId + " - 재고 부족");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        
        System.out.println("\n===== 정확한 재고 소진 테스트 결과 =====");
        System.out.println("초기 재고: 10");
        System.out.println("주문 시도: " + numberOfOrders);
        System.out.println("성공한 주문: " + successCount.get());
        System.out.println("실패한 주문: " + failCount.get());
        System.out.println("최종 재고: " + finalProduct.getStock());
        System.out.println("======================================");
        
        // 정확히 10개만 판매되고 재고가 0이 되어야 함
        assertEquals(10, successCount.get(), "정확히 10개의 주문만 성공해야 합니다");
        assertEquals(5, failCount.get(), "5개의 주문은 재고 부족으로 실패해야 합니다");
        assertEquals(0, finalProduct.getStock(), "재고는 정확히 0이어야 합니다");
        
        System.out.println("✓ 정확한 재고 소진 테스트 통과!");
    }
}
