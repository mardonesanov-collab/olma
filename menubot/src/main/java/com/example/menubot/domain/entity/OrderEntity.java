package com.example.menubot.domain.entity;

import com.example.menubot.domain.enums.OrderStatus;
import com.example.menubot.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "table_number")
    private String tableNumber;

    @Column(name = "total_products_price", nullable = false)
    private Long totalProductsPrice;

    @Column(name = "service_fee_price", nullable = false)
    private Long serviceFeePrice;

    @Column(name = "final_price", nullable = false)
    private Long finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

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
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    public Long getTotalProductsPrice() { return totalProductsPrice; }
    public void setTotalProductsPrice(Long totalProductsPrice) { this.totalProductsPrice = totalProductsPrice; }
    public Long getServiceFeePrice() { return serviceFeePrice; }
    public void setServiceFeePrice(Long serviceFeePrice) { this.serviceFeePrice = serviceFeePrice; }
    public Long getFinalPrice() { return finalPrice; }
    public void setFinalPrice(Long finalPrice) { this.finalPrice = finalPrice; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItemEntity> getItems() { return items; }
    public void setItems(List<OrderItemEntity> items) { this.items = items; }
}
