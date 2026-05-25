package com.example.menubot.service;

import com.example.menubot.domain.entity.*;
import com.example.menubot.domain.enums.OrderStatus;
import com.example.menubot.domain.enums.PaymentStatus;
import com.example.menubot.domain.repository.AppUserRepository;
import com.example.menubot.domain.repository.OrderEntityRepository;
import com.example.menubot.domain.repository.ProductEntityRepository;
import com.example.menubot.domain.repository.RestaurantEntityRepository;
import com.example.menubot.domain.repository.ReviewEntityRepository;
import com.example.menubot.exception.ApiException;
import com.example.menubot.security.AuthContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderSaasService {

    private final OrderEntityRepository orderRepository;
    private final ProductEntityRepository productRepository;
    private final RestaurantEntityRepository restaurantRepository;
    private final ReviewEntityRepository reviewRepository;
    private final AppUserRepository userRepository;
    private final TenantAccessService tenantAccess;
    private final TelegramNotificationService notifications;

    public OrderSaasService(OrderEntityRepository orderRepository,
                            ProductEntityRepository productRepository,
                            RestaurantEntityRepository restaurantRepository,
                            ReviewEntityRepository reviewRepository,
                            AppUserRepository userRepository,
                            TenantAccessService tenantAccess,
                            TelegramNotificationService notifications) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.restaurantRepository = restaurantRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.tenantAccess = tenantAccess;
        this.notifications = notifications;
    }

    @Transactional
    public OrderEntity placeOrder(Long restaurantId, String tableNumber, List<OrderLineRequest> lines) {
        RestaurantEntity restaurant = tenantAccess.requireApprovedRestaurant(restaurantId);
        AppUser client = AuthContext.require();

        if (lines == null || lines.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        long productsTotal = 0;
        OrderEntity order = new OrderEntity();
        order.setRestaurantId(restaurantId);
        order.setClientId(client.getId());
        order.setTableNumber(tableNumber);
        order.setStatus(OrderStatus.NEW);
        order.setPaymentStatus(PaymentStatus.PENDING);

        for (OrderLineRequest line : lines) {
            ProductEntity product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
            if (!product.getRestaurantId().equals(restaurantId) || !product.isAvailable()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Product unavailable: " + product.getName());
            }
            long lineTotal = product.getPrice() * line.quantity();
            productsTotal += lineTotal;

            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductId(product.getId());
            item.setQuantity(line.quantity());
            item.setPrice(product.getPrice());
            order.getItems().add(item);
        }

        long serviceFee = BigDecimal.valueOf(productsTotal)
                .multiply(restaurant.getServiceFeePercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();

        order.setTotalProductsPrice(productsTotal);
        order.setServiceFeePrice(serviceFee);
        order.setFinalPrice(productsTotal + serviceFee);
        order = orderRepository.save(order);

        notifications.notifyVendorNewOrder(restaurant, order);
        return order;
    }

    public List<OrderEntity> vendorOrders(Long restaurantId) {
        tenantAccess.requireVendorOwner(restaurantId);
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
    }

    @Transactional
    public OrderEntity updateStatus(Long restaurantId, Long orderId, OrderStatus status) {
        tenantAccess.requireVendorOwner(restaurantId);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getRestaurantId().equals(restaurantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        order.setStatus(status);
        order = orderRepository.save(order);

        if (status == OrderStatus.DELIVERED) {
            userRepository.findById(order.getClientId()).ifPresent(client ->
                    notifications.notifyClientReviewRequest(client.getTgId(), restaurantId, orderId));
        }
        return order;
    }

    public void callWaiter(Long restaurantId, String tableNumber) {
        RestaurantEntity restaurant = tenantAccess.requireApprovedRestaurant(restaurantId);
        notifications.notifyVendorCallWaiter(restaurant, tableNumber != null ? tableNumber : "?");
    }

    @Transactional
    public ReviewEntity submitReview(Long restaurantId, Long orderId, short rating, String comment) {
        tenantAccess.requireApprovedRestaurant(restaurantId);
        AppUser client = AuthContext.require();
        if (rating < 1 || rating > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rating must be 1-5");
        }
        ReviewEntity review = new ReviewEntity();
        review.setRestaurantId(restaurantId);
        review.setClientId(client.getId());
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setComment(comment);
        return reviewRepository.save(review);
    }

    public Map<String, Object> analytics(Long restaurantId) {
        tenantAccess.requireVendorOwner(restaurantId);
        Instant now = Instant.now();
        long today = orderRepository.sumRevenueSince(restaurantId, now.truncatedTo(ChronoUnit.DAYS));
        long week = orderRepository.sumRevenueSince(restaurantId, now.minus(7, ChronoUnit.DAYS));
        long month = orderRepository.sumRevenueSince(restaurantId, now.minus(30, ChronoUnit.DAYS));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("todayRevenue", today);
        stats.put("weekRevenue", week);
        stats.put("monthRevenue", month);
        stats.put("todayOrders", orderRepository.countOrdersSince(restaurantId, now.truncatedTo(ChronoUnit.DAYS)));
        stats.put("weekOrders", orderRepository.countOrdersSince(restaurantId, now.minus(7, ChronoUnit.DAYS)));
        stats.put("monthOrders", orderRepository.countOrdersSince(restaurantId, now.minus(30, ChronoUnit.DAYS)));
        return stats;
    }

    public record OrderLineRequest(Long productId, int quantity) {}
}
