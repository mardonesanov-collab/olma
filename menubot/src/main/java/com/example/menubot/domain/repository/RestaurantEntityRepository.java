package com.example.menubot.domain.repository;

import com.example.menubot.domain.entity.RestaurantEntity;
import com.example.menubot.domain.enums.RestaurantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantEntityRepository extends JpaRepository<RestaurantEntity, Long> {
    List<RestaurantEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<RestaurantEntity> findByStatus(RestaurantStatus status);
    Optional<RestaurantEntity> findByUniqueSlug(String uniqueSlug);
    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
}
