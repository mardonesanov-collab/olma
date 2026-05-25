package com.example.menubot.api;

import com.example.menubot.domain.entity.OrderEntity;
import com.example.menubot.domain.entity.ReviewEntity;
import com.example.menubot.service.OrderSaasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client")
public class ClientApiController {

    private final OrderSaasService orderService;

    public ClientApiController(OrderSaasService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/restaurants/{restaurantId}/orders")
    public ResponseEntity<OrderEntity> placeOrder(@PathVariable Long restaurantId,
                                                  @RequestParam(required = false) String tableNumber,
                                                  @RequestBody List<OrderSaasService.OrderLineRequest> items) {
        return ResponseEntity.ok(orderService.placeOrder(restaurantId, tableNumber, items));
    }

    @PostMapping("/restaurants/{restaurantId}/call-waiter")
    public ResponseEntity<Map<String, Boolean>> callWaiter(@PathVariable Long restaurantId,
                                                           @RequestParam String tableNumber) {
        orderService.callWaiter(restaurantId, tableNumber);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/restaurants/{restaurantId}/reviews")
    public ResponseEntity<ReviewEntity> review(@PathVariable Long restaurantId,
                                                 @RequestParam(required = false) Long orderId,
                                                 @RequestParam short rating,
                                                 @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(orderService.submitReview(restaurantId, orderId, rating, comment));
    }
}
