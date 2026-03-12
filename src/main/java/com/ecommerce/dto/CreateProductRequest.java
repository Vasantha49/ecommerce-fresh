package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateProductRequest {
    @NotBlank private String name;
    private String description;
    @NotBlank private String sku;
    @NotNull @DecimalMin("0.01") private BigDecimal price;
    @NotBlank private String category;
    @Min(0) private Integer stockQuantity = 0;
    private String imageUrl;
    private String brand;
    @DecimalMin("0.0") @DecimalMax("100.0") private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal weight;
}
