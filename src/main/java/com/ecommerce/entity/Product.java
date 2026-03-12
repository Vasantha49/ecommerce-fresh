package com.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_sku", columnList = "sku", unique = true),
    @Index(name = "idx_product_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true, length = 50)
    @NotBlank
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    @Column(nullable = false)
    @NotBlank
    private String category;

    @Column(nullable = false)
    @Min(0)
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    @Min(0)
    private Integer reservedQuantity = 0;

    private String imageUrl;

    private String brand;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, precision = 5, scale = 2)
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    public int getAvailableQuantity() {
        return Math.max(0, stockQuantity - reservedQuantity);
    }

    public boolean isInStock(int requested) {
        return getAvailableQuantity() >= requested;
    }
}
