package com.example.menubot.domain.entity;

import com.example.menubot.domain.enums.RestaurantStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "restaurants")
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatus status = RestaurantStatus.PENDING;

    @Column(name = "service_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal serviceFeePercent = BigDecimal.valueOf(10);

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "unique_slug", unique = true)
    private String uniqueSlug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
    public RestaurantStatus getStatus() { return status; }
    public void setStatus(RestaurantStatus status) { this.status = status; }
    public BigDecimal getServiceFeePercent() { return serviceFeePercent; }
    public void setServiceFeePercent(BigDecimal serviceFeePercent) { this.serviceFeePercent = serviceFeePercent; }
    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }
    public String getUniqueSlug() { return uniqueSlug; }
    public void setUniqueSlug(String uniqueSlug) { this.uniqueSlug = uniqueSlug; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
