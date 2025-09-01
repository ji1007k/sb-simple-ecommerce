package com.jikim.ecommerce;

import com.jikim.ecommerce.dto.OrderRequest;
import com.jikim.ecommerce.entity.Product;
import com.jikim.ecommerce.repository.ProductRepository;
import com.jikim.ecommerce.service.CartService;
import com.jikim.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL 로그 없이 깔끔한 출력만 보여주는 간단한 동시성 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SimpleConcurrencyTest {
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ProductRepository productRepository;
    
    private Product testProduct;
    
    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .name("인기 상품")
                .description("품절 임박 상품")
                .price(new BigDecimal("50000"))
                .stock(20) // 재고 20개만
                .category("한정판")
                .build();
        testProduct = productRepository.save(testProduct);
    }
    
    @Test
    @DisplayName("🔥 블랙프라이데이 시뮬레이션 - 20개 한정 상품을 50명이 구매 시도")
    void blackFridaySimulation() throws InterruptedException {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🛒 블랙프라이데이 세일 시뮬레이션");
        System.out.println("=".repeat(60));
        System.out.println("📦 상품명: " + testProduct.getName());
        System.out.println("💰 가격: " + testProduct.getPrice() + "원");
        System.out.println("📊 한정 수량: " + testProduct.getStock() + "개");
        System.out.println("👥 구매 시도 인원: 50명");
        System.out.println("=".repeat(60) + "\n");
        
        Thread.sleep(1000); // 드라마틱 효과
        System.out.println("⏰ 세일 시작! (3초 후 종료)\n");
        
        int buyers = 50; // 50명이 동시 구매 시도
        ExecutorService executor = Executors.newFixedThreadPool(buyers);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(buyers);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        // 50명의 구매자 준비
        for (int i = 1; i <= buyers; i++) {
            final int buyerId = i;
            executor.submit(() -> {
                try {
                    startSignal.await(); // 모두 동시 시작 대기
                    
                    String sessionId = "buyer-" + buyerId;
                    cartService.addToCart(sessionId, testProduct.getId(), 1);
                    
                    OrderRequest order = new OrderRequest();
                    order.setCustomerName("구매자" + buyerId);
                    order.setCustomerEmail("buyer" + buyerId + "@test.com");
                    order.setShippingAddress("주소 " + buyerId);
                    
                    try {
                        orderService.createOrder(sessionId, order);
                        int count = successCount.incrementAndGet();
                        System.out.printf("✅ [%2d번째 성공] 구매자%d님이 구매에 성공했습니다!\n", count, buyerId);
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        if (failCount.get() <= 3) { // 처음 3명만 실패 메시지 출력
                            System.out.printf("❌ 구매자%d님 품절로 구매 실패...\n", buyerId);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneSignal.countDown();
                }
            });
        }
        
        Thread.sleep(500);
        startSignal.countDown(); // 동시 시작!
        
        boolean finished = doneSignal.await(3, TimeUnit.SECONDS);
        executor.shutdown();
        
        // 최종 결과
        Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 최종 결과");
        System.out.println("=".repeat(60));
        System.out.println("✅ 구매 성공: " + successCount.get() + "명");
        System.out.println("❌ 구매 실패: " + failCount.get() + "명 (품절)");
        System.out.println("📦 남은 재고: " + finalProduct.getStock() + "개");
        System.out.println("=".repeat(60));
        
        // 검증
        assertEquals(20, successCount.get(), "정확히 20명만 구매 성공해야 함");
        assertEquals(30, failCount.get(), "30명은 품절로 구매 실패해야 함");
        assertEquals(0, finalProduct.getStock(), "재고는 0이어야 함");
        
        System.out.println("\n✨ 테스트 성공! 재고 관리가 완벽하게 작동했습니다.");
    }
    
    @Test
    @DisplayName("⚡ 초간단 동시성 테스트 - 10명이 동시에 구매")
    void simpleQuickTest() throws InterruptedException {
        System.out.println("\n🚀 빠른 동시성 테스트 (10명 동시 구매)");
        System.out.println("-".repeat(40));
        
        testProduct.setStock(5); // 재고 5개만
        productRepository.save(testProduct);
        
        System.out.println("재고: 5개 / 구매자: 10명\n");
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger success = new AtomicInteger(0);
        
        for (int i = 1; i <= 10; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    String sessionId = "quick-" + id;
                    cartService.addToCart(sessionId, testProduct.getId(), 1);
                    
                    OrderRequest order = new OrderRequest();
                    order.setCustomerName("User" + id);
                    order.setCustomerEmail("user" + id + "@test.com");
                    order.setShippingAddress("Addr" + id);
                    
                    orderService.createOrder(sessionId, order);
                    System.out.println("✓ User" + id + " 구매 성공");
                    success.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("✗ User" + id + " 구매 실패 (품절)");
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.println("\n결과: " + success.get() + "명 성공 / " + (10 - success.get()) + "명 실패");
        assertEquals(5, success.get(), "5명만 성공해야 함");
        
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(0, result.getStock(), "재고 0이어야 함");
        
        System.out.println("✅ 테스트 통과!");
    }
}
