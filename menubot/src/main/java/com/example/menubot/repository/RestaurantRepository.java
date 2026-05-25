package com.example.menubot.repository;

import com.example.menubot.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.context.annotation.Profile;
import java.util.Optional;

@Profile("legacy")
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<Restaurant> findByUniqueLink(String uniqueLink);
}