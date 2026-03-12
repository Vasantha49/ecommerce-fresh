package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.entity.Order;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order processing and history")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Checkout — create order from cart")
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest req,
            @RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", orderService.createOrder(userId, req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getById(id)));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getByNumber(orderNumber)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Order history for a user")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getUserOrders(userId, page, size)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id, @RequestParam Order.OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated", orderService.updateStatus(id, status)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", orderService.cancel(id, userId)));
    }
}
