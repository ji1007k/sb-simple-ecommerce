package com.jikim.ecommerce.repository;

import com.jikim.ecommerce.entity.SampleData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

@Repository
public interface SampleDataRepository extends JpaRepository<SampleData, Long> {
    
    @Query("SELECT COUNT(s) FROM SampleData s")
    long getTotalCount();
    
    /**
     * 페이징을 통한 배치 처리용 조회
     */
    Page<SampleData> findAllByOrderById(Pageable pageable);
    
    /**
     * 스트림을 통한 메모리 효율적 조회 (Cursor 기반)
     * 주의: @Transactional(readOnly = true) 필수
     */
    @Query("SELECT s FROM SampleData s ORDER BY s.id")
    Stream<SampleData> findAllByOrderByIdStream();
}
