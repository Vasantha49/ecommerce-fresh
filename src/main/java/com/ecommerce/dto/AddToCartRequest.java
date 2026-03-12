package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddToCartRequest {
    @NotNull private Long productId;
    @NotNull @Min(1) private Integer quantity;
    private String sessionId;
}
