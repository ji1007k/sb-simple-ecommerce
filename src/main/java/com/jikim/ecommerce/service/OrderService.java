package com.jikim.ecommerce.service;

import com.jikim.ecommerce.dto.OrderRequest;
import com.jikim.ecommerce.entity.Cart;
import com.jikim.ecommerce.entity.Order;
import com.jikim.ecommerce.entity.OrderItem;
import com.jikim.ecommerce.entity.Product;
import com.jikim.ecommerce.repository.OrderRepository;
import com.jikim.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    
    @Retryable(
        value = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public Order createOrder(String sessionId, OrderRequest orderRequest) {
        Cart cart = cartService.getOrCreateCart(sessionId);
        
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        Order order = Order.builder()
                .customerName(orderRequest.getCustomerName())
                .customerEmail(orderRequest.getCustomerEmail())
                .shippingAddress(orderRequest.getShippingAddress())
                .status(Order.OrderStatus.PENDING)
                .build();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        // 재고 확인 및 감소
        for (var cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // 재고 감소 (동시성 제어)
            product.decreaseStock(cartItem.getQuantity());
            productRepository.save(product);
            
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();
            
            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(
                    product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
            );
        }
        
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        
        // Clear cart after order
        cartService.clearCart(sessionId);
        
        log.info("Order created successfully: {}", savedOrder.getId());
        return savedOrder;
    }
    
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByEmail(String email) {
        return orderRepository.findByCustomerEmail(email);
    }
    
    public Order updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
