package com.jikim.ecommerce;

import com.jikim.ecommerce.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class StockManagementTest {
    
    private Product product;
    
    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .description("단위 테스트용 상품")
                .price(new BigDecimal("10000"))
                .stock(50)
                .category("테스트")
                .build();
    }
    
    @Test
    @DisplayName("정상적으로 재고가 감소해야 한다")
    void decreaseStock_Success() {
        // given
        int quantity = 10;
        int initialStock = product.getStock();
        
        // when
        product.decreaseStock(quantity);
        
        // then
        assertEquals(initialStock - quantity, product.getStock());
        assertEquals(40, product.getStock());
    }
    
    @Test
    @DisplayName("재고보다 많은 수량을 요청하면 예외가 발생해야 한다")
    void decreaseStock_InsufficientStock_ThrowsException() {
        // given
        int quantity = 60; // 재고(50)보다 많은 수량
        
        // when & then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> product.decreaseStock(quantity)
        );
        
        assertTrue(exception.getMessage().contains("재고가 부족합니다"));
        assertEquals(50, product.getStock()); // 재고는 변경되지 않아야 함
    }
    
    @Test
    @DisplayName("재고와 정확히 같은 수량을 요청하면 재고가 0이 되어야 한다")
    void decreaseStock_ExactAmount() {
        // given
        int quantity = 50;
        
        // when
        product.decreaseStock(quantity);
        
        // then
        assertEquals(0, product.getStock());
    }
    
    @Test
    @DisplayName("재고가 증가해야 한다")
    void increaseStock_Success() {
        // given
        int quantity = 20;
        int initialStock = product.getStock();
        
        // when
        product.increaseStock(quantity);
        
        // then
        assertEquals(initialStock + quantity, product.getStock());
        assertEquals(70, product.getStock());
    }
    
    @Test
    @DisplayName("0개를 감소시켜도 재고는 변하지 않아야 한다")
    void decreaseStock_Zero() {
        // given
        int initialStock = product.getStock();
        
        // when
        product.decreaseStock(0);
        
        // then
        assertEquals(initialStock, product.getStock());
    }
    
    @Test
    @DisplayName("음수 수량으로 재고 감소 시도시 예외가 발생해야 한다")
    void decreaseStock_NegativeQuantity() {
        // given
        int quantity = -10;
        
        // when & then
        // 음수는 재고보다 작은 값이므로 예외 발생
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> product.decreaseStock(quantity)
        );
        
        assertTrue(exception.getMessage().contains("재고가 부족합니다"));
    }
}
