package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private String category;
    private Integer stockQuantity;
    private Integer availableQuantity;
    private String imageUrl;
    private String brand;
    private Boolean active;
    private BigDecimal taxRate;
    private LocalDateTime createdAt;
}
