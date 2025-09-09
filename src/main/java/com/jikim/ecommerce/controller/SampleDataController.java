package com.jikim.ecommerce.controller;

import com.jikim.ecommerce.service.SampleDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sample-data")
@RequiredArgsConstructor
@Slf4j
public class SampleDataController {
    
    private final SampleDataService sampleDataService;
    
    /**
     * 테스트용 데이터 생성
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateData(
            @RequestParam(defaultValue = "10000") int count) {
        
        try {
            if (count > 100000) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "최대 100,000건까지만 생성 가능합니다."));
            }
            
            sampleDataService.generateSampleData(count);
            
            return ResponseEntity.ok(Map.of(
                    "message", String.format("%d건의 테스트 데이터가 생성되었습니다.", count),
                    "totalCount", sampleDataService.getDataCount()
            ));
        } catch (Exception e) {
            log.error("Failed to generate sample data", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 데이터 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getDataCount() {
        try {
            long count = sampleDataService.getDataCount();
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            log.error("Failed to get data count", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 모든 데이터 삭제
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearAllData() {
        try {
            sampleDataService.clearAllData();
            return ResponseEntity.ok(Map.of("message", "모든 데이터가 삭제되었습니다."));
        } catch (Exception e) {
            log.error("Failed to clear data", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
