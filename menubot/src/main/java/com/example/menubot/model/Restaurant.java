package com.example.menubot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

// @Entity — legacy disabled (SaaS: RestaurantEntity)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String photoPath;

    @Column(unique = true, nullable = false)
    private String uniqueLink;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Restaurant() {}

    public Restaurant(Long ownerId, String name) {
        this.ownerId = ownerId;
        this.name = name;
        this.uniqueLink = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public String getUniqueLink() { return uniqueLink; }
    public void setUniqueLink(String uniqueLink) { this.uniqueLink = uniqueLink; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}