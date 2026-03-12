package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockUpdateRequest {
    @NotNull @Min(1) private Integer quantity;
    @NotBlank private String reason;
}
