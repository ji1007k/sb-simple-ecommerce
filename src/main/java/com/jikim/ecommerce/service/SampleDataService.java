package com.jikim.ecommerce.service;

import com.jikim.ecommerce.entity.SampleData;
import com.jikim.ecommerce.repository.SampleDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class SampleDataService {
    
    private final SampleDataRepository sampleDataRepository;
    private final Random random = new Random();
    
    private final String[] categories = {
            "전자제품", "의류", "식품", "도서", "스포츠", "가구", "화장품", "장난감", "악기", "자동차용품"
    };
    
    private final String[] adjectives = {
            "프리미엄", "고급", "베스트", "인기", "신상", "특가", "한정판", "추천", "최신", "클래식"
    };
    
    private final String[] nouns = {
            "스마트폰", "노트북", "티셔츠", "청바지", "과자", "음료", "책", "운동화", "가방", "시계",
            "헤드폰", "키보드", "마우스", "모니터", "의자", "책상", "램프", "쿠션", "베개", "이불"
    };
    
    /**
     * 테스트용 대량 데이터 생성
     */
    @Transactional
    public void generateSampleData(int count) {
        log.info("Generating {} sample data records...", count);
        
        List<SampleData> dataList = new ArrayList<>();
        int batchSize = 1000;
        
        for (int i = 0; i < count; i++) {
            SampleData data = SampleData.builder()
                    .name(generateRandomName())
                    .description(generateRandomDescription())
                    .price(generateRandomPrice())
                    .category(categories[random.nextInt(categories.length)])
                    .build();
            
            dataList.add(data);
            
            // 배치 단위로 저장
            if (dataList.size() == batchSize || i == count - 1) {
                sampleDataRepository.saveAll(dataList);
                dataList.clear();
                log.info("Saved batch. Progress: {}/{}", i + 1, count);
            }
        }
        
        log.info("Sample data generation completed: {} records", count);
    }
    
    /**
     * 모든 샘플 데이터 삭제
     */
    @Transactional
    public void clearAllData() {
        long count = sampleDataRepository.count();
        sampleDataRepository.deleteAll();
        log.info("Cleared all sample data: {} records deleted", count);
    }
    
    /**
     * 현재 데이터 개수 조회
     */
    public long getDataCount() {
        return sampleDataRepository.count();
    }
    
    private String generateRandomName() {
        String adjective = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];
        return adjective + " " + noun;
    }
    
    private String generateRandomDescription() {
        String[] templates = {
                "%s 제품으로 고객 만족도가 높습니다.",
                "품질이 우수하고 내구성이 뛰어난 %s입니다.",
                "최신 기술이 적용된 혁신적인 %s 제품입니다.",
                "합리적인 가격의 실용적인 %s입니다.",
                "디자인과 기능성을 모두 갖춘 %s입니다."
        };
        
        String template = templates[random.nextInt(templates.length)];
        String productType = nouns[random.nextInt(nouns.length)];
        return String.format(template, productType);
    }
    
    private Integer generateRandomPrice() {
        // 1,000원 ~ 500,000원 범위의 랜덤 가격
        int basePrice = random.nextInt(500) + 1; // 1~500
        return basePrice * 1000;
    }
}
