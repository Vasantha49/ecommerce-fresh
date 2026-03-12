package com.ecommerce.controller;

import com.ecommerce.dto.*;
import com.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get cart by session ID")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable String sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(sessionId)));
    }

    @PostMapping("/add")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> add(@Valid @RequestBody AddToCartRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cartService.addToCart(req)));
    }

    @PutMapping("/{sessionId}/items/{productId}")
    @Operation(summary = "Update item quantity (0 = remove)")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable String sessionId, @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.updateItem(sessionId, productId, req.getQuantity())));
    }

    @DeleteMapping("/{sessionId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable String sessionId, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.removeItem(sessionId, productId)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}
