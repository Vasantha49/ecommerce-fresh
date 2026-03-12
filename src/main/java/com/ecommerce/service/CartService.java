package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public CartResponse getCart(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId).orElseGet(() -> createCart(sessionId));
        return toResponse(cart);
    }

    public CartResponse addToCart(AddToCartRequest req) {
        String sessionId = req.getSessionId() != null ? req.getSessionId() : UUID.randomUUID().toString();
        Cart cart = cartRepository.findBySessionId(sessionId).orElseGet(() -> createCart(sessionId));

        Product product = productService.getEntity(req.getProductId());
        if (!product.getActive()) throw new RuntimeException("Product not available: " + product.getName());
        if (!product.isInStock(req.getQuantity()))
            throw new RuntimeException(String.format("Insufficient stock for '%s': available %d",
                    product.getName(), product.getAvailableQuantity()));

        cart.addItem(CartItem.builder().product(product).quantity(req.getQuantity())
                .unitPrice(product.getPrice()).build());
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse updateItem(String sessionId, Long productId, int quantity) {
        Cart cart = getCartEntity(sessionId);
        if (quantity == 0) {
            cart.removeItem(productId);
        } else {
            cart.getItems().stream().filter(i -> i.getProduct().getId().equals(productId))
                    .findFirst().ifPresent(item -> {
                        if (!item.getProduct().isInStock(quantity))
                            throw new RuntimeException("Insufficient stock: available " + item.getProduct().getAvailableQuantity());
                        item.setQuantity(quantity);
                    });
        }
        return toResponse(cartRepository.save(cart));
    }

    public CartResponse removeItem(String sessionId, Long productId) {
        Cart cart = getCartEntity(sessionId);
        cart.removeItem(productId);
        return toResponse(cartRepository.save(cart));
    }

    public void clearCart(String sessionId) {
        cartRepository.findBySessionId(sessionId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    public Cart getCartEntity(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for session: " + sessionId));
    }

    private Cart createCart(String sessionId) {
        return cartRepository.save(Cart.builder()
                .sessionId(sessionId)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
    }

    public CartResponse toResponse(Cart cart) {
        return CartResponse.builder()
                .id(cart.getId()).sessionId(cart.getSessionId())
                .items(cart.getItems().stream().map(i -> CartItemResponse.builder()
                        .productId(i.getProduct().getId()).productName(i.getProduct().getName())
                        .productSku(i.getProduct().getSku()).imageUrl(i.getProduct().getImageUrl())
                        .quantity(i.getQuantity()).unitPrice(i.getUnitPrice()).subtotal(i.getSubtotal())
                        .build()).collect(Collectors.toList()))
                .totalPrice(cart.getTotalPrice()).totalItems(cart.getTotalItems())
                .updatedAt(cart.getUpdatedAt()).build();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredCarts() {
        int deleted = cartRepository.deleteExpiredCarts(LocalDateTime.now());
        if (deleted > 0) log.info("Cleaned up {} expired carts", deleted);
    }
}
