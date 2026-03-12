package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateCartItemRequest {
    @NotNull @Min(0) private Integer quantity;
}
