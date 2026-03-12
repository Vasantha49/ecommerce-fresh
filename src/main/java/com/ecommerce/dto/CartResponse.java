package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartResponse {
    private Long id;
    private String sessionId;
    private List<CartItemResponse> items;
    private BigDecimal totalPrice;
    private Integer totalItems;
    private LocalDateTime updatedAt;
}
