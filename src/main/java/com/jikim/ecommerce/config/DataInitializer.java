package com.jikim.ecommerce.config;

import com.jikim.ecommerce.entity.Product;
import com.jikim.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    
    @Bean
    CommandLineRunner init(ProductRepository productRepository) {
        return args -> {
            // 샘플 상품 데이터 생성
            Product laptop = Product.builder()
                    .name("노트북 Pro 15")
                    .description("고성능 15인치 노트북")
                    .price(new BigDecimal("1500000"))
                    .stock(50)
                    .category("전자제품")
                    .imageUrl("https://example.com/laptop.jpg")
                    .build();
            
            Product mouse = Product.builder()
                    .name("무선 마우스")
                    .description("인체공학적 무선 마우스")
                    .price(new BigDecimal("35000"))
                    .stock(100)
                    .category("전자제품")
                    .imageUrl("https://example.com/mouse.jpg")
                    .build();
            
            Product keyboard = Product.builder()
                    .name("기계식 키보드")
                    .description("RGB 백라이트 기계식 키보드")
                    .price(new BigDecimal("120000"))
                    .stock(75)
                    .category("전자제품")
                    .imageUrl("https://example.com/keyboard.jpg")
                    .build();
            
            Product monitor = Product.builder()
                    .name("27인치 모니터")
                    .description("4K UHD 27인치 모니터")
                    .price(new BigDecimal("450000"))
                    .stock(30)
                    .category("전자제품")
                    .imageUrl("https://example.com/monitor.jpg")
                    .build();
            
            Product tshirt = Product.builder()
                    .name("베이직 티셔츠")
                    .description("100% 면 베이직 티셔츠")
                    .price(new BigDecimal("25000"))
                    .stock(200)
                    .category("의류")
                    .imageUrl("https://example.com/tshirt.jpg")
                    .build();
            
            Product jeans = Product.builder()
                    .name("슬림핏 청바지")
                    .description("스트레치 슬림핏 청바지")
                    .price(new BigDecimal("65000"))
                    .stock(150)
                    .category("의류")
                    .imageUrl("https://example.com/jeans.jpg")
                    .build();
            
            Product sneakers = Product.builder()
                    .name("러닝화")
                    .description("경량 러닝화")
                    .price(new BigDecimal("89000"))
                    .stock(80)
                    .category("신발")
                    .imageUrl("https://example.com/sneakers.jpg")
                    .build();
            
            Product book = Product.builder()
                    .name("자바 프로그래밍 입문")
                    .description("초보자를 위한 자바 프로그래밍 가이드")
                    .price(new BigDecimal("32000"))
                    .stock(50)
                    .category("도서")
                    .imageUrl("https://example.com/book.jpg")
                    .build();
            
            Product headphones = Product.builder()
                    .name("무선 헤드폰")
                    .description("노이즈 캔슬링 무선 헤드폰")
                    .price(new BigDecimal("250000"))
                    .stock(40)
                    .category("전자제품")
                    .imageUrl("https://example.com/headphones.jpg")
                    .build();
            
            Product backpack = Product.builder()
                    .name("노트북 백팩")
                    .description("15인치 노트북 수납 가능 백팩")
                    .price(new BigDecimal("55000"))
                    .stock(60)
                    .category("가방")
                    .imageUrl("https://example.com/backpack.jpg")
                    .build();
            
            productRepository.saveAll(Arrays.asList(
                    laptop, mouse, keyboard, monitor, tshirt,
                    jeans, sneakers, book, headphones, backpack
            ));
            
            System.out.println("샘플 데이터가 초기화되었습니다!");
        };
    }
}
