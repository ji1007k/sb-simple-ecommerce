package com.jikim.ecommerce.dto;

import com.jikim.ecommerce.entity.Cart;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class CartResponse {
    private Long id;
    private String sessionId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(CartItemResponse::from)
                .collect(Collectors.toList());
        
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return CartResponse.builder()
                .id(cart.getId())
                .sessionId(cart.getSessionId())
                .items(items)
                .totalAmount(total)
                .createdAt(cart.getCreatedAt())
                .build();
    }
}
