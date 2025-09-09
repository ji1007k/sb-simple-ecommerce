package com.jikim.ecommerce.repository;

import com.jikim.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByCustomerEmail(String email);
    
    // 최근 주문 조회 (N+1 문제 발생)
    List<Order> findTop10ByOrderByIdDesc();
    
    // @EntityGraph를 사용한 조회 (3단계에서 사용)
    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("SELECT o FROM Order o ORDER BY o.id DESC LIMIT 10")
    List<Order> findTop10WithItemsAndProducts();
    
    // JOIN FETCH를 사용한 최적화 조회 (5단계에서 사용) - 성능 개선
    @Query(value = "SELECT DISTINCT o FROM Order o " +
           "WHERE o.id IN (SELECT o2.id FROM Order o2 ORDER BY o2.id DESC LIMIT 10)")
    List<Order> findTop10OrderIds();
    
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.items oi " +
           "JOIN FETCH oi.product p " +
           "WHERE o.id IN :orderIds " +
           "ORDER BY o.id DESC")
    List<Order> findTop10WithJoinFetch(@Param("orderIds") List<Long> orderIds);
    
    // 간단한 JOIN FETCH (비교용)
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.items oi " +
           "JOIN FETCH oi.product p " +
           "WHERE o.id IN (SELECT o2.id FROM Order o2 ORDER BY o2.id DESC LIMIT 10) " +
           "ORDER BY o.id DESC")
    List<Order> findTop10WithSimpleJoinFetch();
    
    // 특정 주문들을 JOIN FETCH로 조회
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.items oi " +
           "JOIN FETCH oi.product p " +
           "WHERE o.id IN :orderIds")
    List<Order> findOrdersWithItemsAndProducts(@Param("orderIds") List<Long> orderIds);
    
    // 일반 조회 vs EntityGraph 성능 비교용
    @EntityGraph(attributePaths = {"items.product"})
    List<Order> findByIdIn(List<Long> orderIds);
}
