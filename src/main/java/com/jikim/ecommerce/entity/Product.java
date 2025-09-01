package com.jikim.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stock;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private String category;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Version
    private Long version;  // 낙관적 락을 위한 버전 필드
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // 재고 감소 메서드
    public void decreaseStock(Integer quantity) {
        if (this.stock < quantity || quantity < 0) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + this.stock + ", 요청 수량: " + quantity);
        }
        this.stock -= quantity;
    }
    
    // 재고 증가 메서드
    public void increaseStock(Integer quantity) {
        this.stock += quantity;
    }
}
