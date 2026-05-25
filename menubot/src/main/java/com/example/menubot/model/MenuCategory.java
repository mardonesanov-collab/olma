package com.example.menubot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// @Entity legacy disabled
// @Table(name = "menu_categories")
public class MenuCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long restaurantId;

    @Column(nullable = false)
    private String name;

    private int sortOrder;
    private LocalDateTime createdAt;

    public MenuCategory() {}

    public MenuCategory(Long restaurantId, String name, int sortOrder) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}