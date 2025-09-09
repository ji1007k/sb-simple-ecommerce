package com.jikim.ecommerce.controller;

import com.jikim.ecommerce.service.DataGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/sample-data")
@RequiredArgsConstructor
@Slf4j
public class DataGenerationController {

    private final DataGenerationService dataGenerationService;

    /**
     * 대용량 샘플 데이터 전체 생성
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createSampleData() {
        try {
            log.info("대용량 샘플 데이터 생성 요청 시작");
            
            dataGenerationService.generateLargeTestData();
            
            return ResponseEntity.ok(Map.of("message", "대용량 샘플 데이터 생성이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("샘플 데이터 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "샘플 데이터 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 상품 샘플 데이터 생성 (비동기)
     */
    @PostMapping("/products")
    public ResponseEntity<Map<String, String>> createProductSamples(
            @RequestParam(defaultValue = "100000") int count) {
        try {
            log.info("상품 샘플 데이터 {}개 생성 요청 (비동기 처리)", count);

            // 비동기로 실행
            CompletableFuture.runAsync(() -> {
                try {
                    dataGenerationService.generateProducts(count);
                    log.info("비동기 상품 데이터 생성 완료: {}개", count);
                } catch (Exception e) {
                    log.error("비동기 상품 데이터 생성 실패", e);
                }
            });
            
            return ResponseEntity.ok(Map.of(
                "message", String.format("상품 샘플 데이터 %d개 생성을 시작했습니다. 로그를 확인해주세요.", count),
                "status", "processing"
            ));
            
        } catch (Exception e) {
            log.error("상품 샘플 데이터 생성 요청 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "상품 샘플 데이터 생성 요청 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 샘플 데이터 생성
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, String>> createOrderSamples(
            @RequestParam(defaultValue = "50000") int count) {
        try {
            log.info("주문 샘플 데이터 {}개 생성 요청", count);
            dataGenerationService.generateOrders(count);
            
            return ResponseEntity.ok(Map.of("message", String.format("주문 샘플 데이터 %d개가 생성되었습니다.", count)));
            
        } catch (Exception e) {
            log.error("주문 샘플 데이터 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "주문 샘플 데이터 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 장바구니 샘플 데이터 생성
     */
    @PostMapping("/carts")
    public ResponseEntity<Map<String, String>> createCartSamples(
            @RequestParam(defaultValue = "10000") int count) {
        try {
            log.info("장바구니 샘플 데이터 {}개 생성 요청", count);
            dataGenerationService.generateCarts(count);
            
            return ResponseEntity.ok(Map.of("message", String.format("장바구니 샘플 데이터 %d개가 생성되었습니다.", count)));
            
        } catch (Exception e) {
            log.error("장바구니 샘플 데이터 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "장바구니 샘플 데이터 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 현재 샘플 데이터 상태 조회
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSampleDataStatus() {
        dataGenerationService.showDataCounts();
        
        return ResponseEntity.ok(Map.of(
                "message", "현재 샘플 데이터 개수를 로그에서 확인해주세요.",
                "note", "추후 JSON 응답으로 개선 예정"
        ));
    }

    /**
     * 대용량 샘플 데이터 생성 (많은 양)
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, String>> createBulkSampleData() {
        try {
            log.info("대용량 샘플 데이터 생성 시작");
            
            dataGenerationService.generateLargeTestData();
            
            return ResponseEntity.ok(Map.of("message", "대용량 샘플 데이터 생성이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("대용량 샘플 데이터 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "대용량 샘플 데이터 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 소규모 샘플 데이터 생성 (빠른 테스트용)
     */
    @PostMapping("/small")
    public ResponseEntity<Map<String, String>> createSmallSampleData() {
        try {
            log.info("소규모 샘플 데이터 생성 시작");
            
            // 소규모: 상품 1만개, 주문 5천개, 장바구니 1천개
            dataGenerationService.generateProducts(10_000);
            dataGenerationService.generateOrders(5_000);
            dataGenerationService.generateCarts(1_000);
            
            return ResponseEntity.ok(Map.of("message", "소규모 샘플 데이터 생성이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("소규모 샘플 데이터 생성 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "소규모 샘플 데이터 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 샘플 데이터 삭제 (개발용)
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteSampleData() {
        try {
            log.info("샘플 데이터 삭제 요청");
            
            // TODO: 실제 삭제 로직 구현
            return ResponseEntity.ok(Map.of("message", "샘플 데이터 삭제 기능은 추후 구현 예정입니다."));
            
        } catch (Exception e) {
            log.error("샘플 데이터 삭제 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "샘플 데이터 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
