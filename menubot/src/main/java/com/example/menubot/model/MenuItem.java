package com.example.menubot.model;

import jakarta.persistence.*;

// @Entity legacy disabled
// @Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long restaurantId;
    private Long categoryId;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private Long price;

    private String photoPath;     // local file name
    private String photoFileId;   // Telegram file_id

    private boolean available = true;
    private int sortOrder;

    public MenuItem() {}

    public MenuItem(Long restaurantId, Long categoryId, String name) {
        this.restaurantId = restaurantId;
        this.categoryId = categoryId;
        this.name = name;
        this.available = true;
    }

    // ===== Getters / Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getPhotoFileId() { return photoFileId; }
    public void setPhotoFileId(String photoFileId) { this.photoFileId = photoFileId; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}