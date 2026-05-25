package com.example.menubot.domain.repository;

import com.example.menubot.domain.entity.OrderEntity;
import com.example.menubot.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    List<OrderEntity> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.finalPrice), 0) FROM OrderEntity o WHERE o.restaurantId = :rid AND o.createdAt >= :from AND o.status <> 'CANCELLED'")
    Long sumRevenueSince(@Param("rid") Long restaurantId, @Param("from") Instant from);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.restaurantId = :rid AND o.createdAt >= :from AND o.status <> 'CANCELLED'")
    long countOrdersSince(@Param("rid") Long restaurantId, @Param("from") Instant from);
}
