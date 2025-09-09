package com.jikim.ecommerce.service;

import com.jikim.ecommerce.entity.*;
import com.jikim.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataGenerationService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    
    private final Random random = new Random();
    
    // 상품 카테고리
    private final String[] categories = {
        "Electronics", "Clothing", "Books", "Home & Garden", "Sports", 
        "Beauty", "Toys", "Automotive", "Health", "Food"
    };
    
    // 상품명 prefix
    private final String[] productPrefixes = {
        "Premium", "Deluxe", "Standard", "Basic", "Pro", "Ultra", 
        "Advanced", "Classic", "Modern", "Smart", "Eco", "Luxury"
    };
    
    // 상품명 suffix  
    private final String[] productSuffixes = {
        "Device", "Tool", "Kit", "Set", "Collection", "Bundle", 
        "Package", "System", "Solution", "Equipment", "Accessory"
    };

    /**
     * 대용량 테스트 데이터 생성 메인 메서드
     */
    public void generateLargeTestData() {
        log.info("=== 대용량 테스트 데이터 생성 시작 ===");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 상품 데이터 생성 (100만개)
            generateProducts(1_000_000);
            
            // 2. 주문 데이터 생성 (50만건)
            generateOrders(500_000);
            
            // 3. 장바구니 데이터 생성 (10만건)
            generateCarts(100_000);
            
            long endTime = System.currentTimeMillis();
            log.info("=== 대용량 테스트 데이터 생성 완료 === 소요시간: {}초", (endTime - startTime) / 1000);
            
        } catch (Exception e) {
            log.error("데이터 생성 중 오류 발생", e);
            throw new RuntimeException("데이터 생성 실패", e);
        }
    }

    /**
     * 상품 데이터 생성
     */
    @Transactional
    public void generateProducts(int count) {
        log.info("상품 데이터 {}개 생성 시작", count);

        StopWatch sw = new StopWatch();
        sw.start();
        
        int batchSize = 1000;
        int totalBatches = (count + batchSize - 1) / batchSize; // 올림 계산
        
        for (int batch = 0; batch < totalBatches; batch++) {
            // 마지막 배치는 남은 개수만큼만 처리
            int currentBatchSize = Math.min(batchSize, count - (batch * batchSize));
            List<Product> products = new ArrayList<>(currentBatchSize);
            
            for (int i = 0; i < currentBatchSize; i++) {
                Product product = createRandomProduct(batch * batchSize + i + 1);
                products.add(product);
            }
            
            productRepository.saveAll(products);
            
            if (batch % 100 == 0 || batch == totalBatches - 1) {
                log.info("상품 생성 진행률: {}/{} 배치 완료", batch + 1, totalBatches);
            }
        }

        sw.stop();
        
        log.info("상품 데이터 생성 완료: {}개. 소요시간: {}ms", count, sw.getTotalTimeMillis());
    }

    /**
     * 주문 데이터 생성 (OrderItem 포함)
     */
    @Transactional
    public void generateOrders(int count) {
        log.info("주문 데이터 {}개 생성 시작", count);

        // 실제 존재하는 상품 ID 목록 조회
        List<Long> productIds = productRepository.findAllIds();
        if (productIds.isEmpty()) {
            throw new IllegalStateException("상품 데이터가 없습니다. 먼저 상품을 생성해주세요.");
        }
        
        log.info("사용 가능한 상품 ID 개수: {}", productIds.size());

        StopWatch sw = new StopWatch();
        sw.start();
        
        // 쿼리 카운터
        int productQueryCount = 0;
        
        int batchSize = 500; // 주문은 OrderItem 때문에 배치 크기를 작게
        int totalBatches = (count + batchSize - 1) / batchSize; // 올림 계산
        
        for (int batch = 0; batch < totalBatches; batch++) {
            // 마지막 배치는 남은 개수만큼만 처리
            int currentBatchSize = Math.min(batchSize, count - (batch * batchSize));
            List<Order> orders = new ArrayList<>(currentBatchSize);
            
            for (int i = 0; i < currentBatchSize; i++) {
                Order order = createRandomOrder(batch * batchSize + i + 1, productIds);
                orders.add(order);
                
                // 각 OrderItem마다 Product 조회 발생 예상
                productQueryCount += order.getItems().size();
            }
            
            orderRepository.saveAll(orders);
            
            if (batch % 50 == 0 || batch == totalBatches - 1) {
                log.info("주문 생성 진행률: {}/{} 배치 완료", batch + 1, totalBatches);
            }
        }

        sw.stop();
        
        log.info("주문 데이터 생성 완료: {}개. 소요시간: {}ms", count, sw.getTotalTimeMillis());
        log.info("⚠️ 예상 Product 조회 쿼리 수: {}번 (N+1 문제!)", productQueryCount);
    }

    /**
     * 장바구니 데이터 생성
     */
    @Transactional
    public void generateCarts(int count) {
        log.info("장바구니 데이터 {}개 생성 시작", count);
        
        List<Long> productIds = productRepository.findAllIds();
        if (productIds.isEmpty()) {
            throw new IllegalStateException("상품 데이터가 없습니다. 먼저 상품을 생성해주세요.");
        }
        
        int batchSize = 500;
        int totalBatches = (count + batchSize - 1) / batchSize; // 올림 계산
        
        for (int batch = 0; batch < totalBatches; batch++) {
            // 마지막 배치는 남은 개수만큼만 처리
            int currentBatchSize = Math.min(batchSize, count - (batch * batchSize));
            List<Cart> carts = new ArrayList<>(currentBatchSize);
            
            for (int i = 0; i < currentBatchSize; i++) {
                Cart cart = createRandomCart(batch * batchSize + i + 1, productIds);
                carts.add(cart);
            }
            
            cartRepository.saveAll(carts);
            
            if (batch % 50 == 0 || batch == totalBatches - 1) {
                log.info("장바구니 생성 진행률: {}/{} 배치 완료", batch + 1, totalBatches);
            }
        }
        
        log.info("장바구니 데이터 생성 완료: {}개", count);
    }

    /**
     * 랜덤 상품 생성
     */
    private Product createRandomProduct(long seed) {
        String category = categories[random.nextInt(categories.length)];
        String prefix = productPrefixes[random.nextInt(productPrefixes.length)];
        String suffix = productSuffixes[random.nextInt(productSuffixes.length)];
        
        return Product.builder()
                .name(String.format("%s %s %s %d", prefix, category, suffix, seed))
                .description(String.format("High quality %s for everyday use. Product ID: %d", category.toLowerCase(), seed))
                .price(generateRandomPrice())
                .stock(random.nextInt(1000) + 10) // 10~1009
                .category(category)
                .imageUrl(String.format("/images/%s_%d.jpg", category.toLowerCase(), seed % 100))
                .build();
    }

    /**
     * 랜덤 주문 생성 (OrderItem 포함)
     */
    private Order createRandomOrder(long seed, List<Long> productIds) {
        String customerName = "Customer_" + seed;
        String customerEmail = String.format("customer%d@example.com", seed);
        
        Order order = Order.builder()
                .customerName(customerName)
                .customerEmail(customerEmail)
                .shippingAddress(String.format("Address %d, City %d, Country", seed, seed % 100))
                .status(getRandomOrderStatus())
                .items(new ArrayList<>())
                .build();
        
        // 주문 생성 시간을 과거 1년 범위로 랜덤 설정
        LocalDateTime createdAt = LocalDateTime.now()
                .minusDays(random.nextInt(365))
                .minusHours(random.nextInt(24))
                .minusMinutes(random.nextInt(60));
        order.setCreatedAt(createdAt);
        
        // OrderItem 생성 (1~5개)
        int itemCount = random.nextInt(5) + 1;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (int i = 0; i < itemCount; i++) {
            // 실제 존재하는 상품 ID에서 랜덤 선택
            Long productId = productIds.get(random.nextInt(productIds.size()));
            
            // 실제 Product 조회 (성능 이슈 체험을 위해)
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            
            int quantity = random.nextInt(3) + 1; // 1~3개
            BigDecimal itemPrice = product.getPrice();
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .price(itemPrice)
                    .build();
            
            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(itemPrice.multiply(BigDecimal.valueOf(quantity)));
        }
        
        order.setTotalAmount(totalAmount);
        return order;
    }

    /**
     * 랜덤 장바구니 생성
     */
    private Cart createRandomCart(long seed, List<Long> productIds) {
        String customerEmail = String.format("cart_customer%d@example.com", seed);
        
        Cart cart = Cart.builder()
                .customerEmail(customerEmail)
                .items(new ArrayList<>())
                .build();
        
        // CartItem 생성 (1~3개)
        int itemCount = random.nextInt(3) + 1;
        
        for (int i = 0; i < itemCount; i++) {
            // 실제 존재하는 상품 ID에서 랜덤 선택
            Long productId = productIds.get(random.nextInt(productIds.size()));
            
            // 실제 Product 조회 (성능 이슈 체험을 위해)
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(random.nextInt(5) + 1) // 1~5개
                    .build();
            
            cart.getItems().add(cartItem);
        }
        
        return cart;
    }

    /**
     * 랜덤 가격 생성 (10원 ~ 100만원)
     */
    private BigDecimal generateRandomPrice() {
        double price = 10 + (1_000_000 - 10) * random.nextDouble();
        return BigDecimal.valueOf(Math.round(price / 10) * 10); // 10원 단위로 반올림
    }

    /**
     * 랜덤 주문 상태 생성
     */
    private Order.OrderStatus getRandomOrderStatus() {
        Order.OrderStatus[] statuses = Order.OrderStatus.values();
        return statuses[random.nextInt(statuses.length)];
    }

    /**
     * 현재 데이터 개수 확인
     */
    public void showDataCounts() {
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();
        long cartCount = cartRepository.count();
        
        log.info("=== 현재 데이터 개수 ===");
        log.info("상품(Product): {}", productCount);
        log.info("주문(Order): {}", orderCount);
        log.info("장바구니(Cart): {}", cartCount);
    }
}
