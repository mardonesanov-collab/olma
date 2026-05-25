package com.example.menubot.domain.repository;

import com.example.menubot.domain.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewEntityRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
}
