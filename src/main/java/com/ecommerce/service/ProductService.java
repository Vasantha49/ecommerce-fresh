package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .filter(Product::getActive)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Cacheable(value = "products", key = "#sku")
    public ProductResponse getBySku(String sku) {
        return productRepository.findBySkuAndActiveTrue(sku)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    public PageResponse<ProductResponse> getAll(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return toPage(productRepository.findByActiveTrue(PageRequest.of(page, size, sort)));
    }

    public PageResponse<ProductResponse> getByCategory(String category, int page, int size) {
        return toPage(productRepository.findByCategoryAndActiveTrue(category, PageRequest.of(page, size, Sort.by("name"))));
    }

    public PageResponse<ProductResponse> search(String query, int page, int size) {
        return toPage(productRepository.searchProducts(query, PageRequest.of(page, size)));
    }

    public List<String> getCategories() { return productRepository.findAllCategories(); }

    public List<ProductResponse> getLowStock(int threshold) {
        return productRepository.findLowStockProducts(threshold).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse create(CreateProductRequest req) {
        if (productRepository.findBySkuAndActiveTrue(req.getSku()).isPresent())
            throw new DuplicateResourceException("Product with SKU already exists: " + req.getSku());
        Product p = Product.builder()
                .name(req.getName()).description(req.getDescription()).sku(req.getSku())
                .price(req.getPrice()).category(req.getCategory())
                .stockQuantity(req.getStockQuantity() != null ? req.getStockQuantity() : 0)
                .imageUrl(req.getImageUrl()).brand(req.getBrand())
                .taxRate(req.getTaxRate() != null ? req.getTaxRate() : BigDecimal.ZERO)
                .weight(req.getWeight()).active(true).build();
        return toResponse(productRepository.save(p));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse update(Long id, UpdateProductRequest req) {
        Product p = getEntity(id);
        if (req.getName() != null)        p.setName(req.getName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getPrice() != null)       p.setPrice(req.getPrice());
        if (req.getCategory() != null)    p.setCategory(req.getCategory());
        if (req.getImageUrl() != null)    p.setImageUrl(req.getImageUrl());
        if (req.getBrand() != null)       p.setBrand(req.getBrand());
        if (req.getActive() != null)      p.setActive(req.getActive());
        if (req.getTaxRate() != null)     p.setTaxRate(req.getTaxRate());
        if (req.getWeight() != null)      p.setWeight(req.getWeight());
        return toResponse(productRepository.save(p));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void addStock(Long id, int quantity) {
        if (productRepository.addStock(id, quantity) == 0)
            throw new ResourceNotFoundException("Product", id);
        log.info("Added {} units to product {}", quantity, id);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        Product p = getEntity(id);
        p.setActive(false);
        productRepository.save(p);
    }

    @Transactional
    public boolean reserveStock(Long productId, int qty) {
        return productRepository.reserveStock(productId, qty) > 0;
    }

    @Transactional
    public void confirmStockDeduction(Long productId, int qty) {
        productRepository.confirmStockDeduction(productId, qty);
    }

    @Transactional
    public void releaseReservedStock(Long productId, int qty) {
        productRepository.releaseReservedStock(productId, qty);
    }

    public Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription()).sku(p.getSku())
                .price(p.getPrice()).category(p.getCategory()).stockQuantity(p.getStockQuantity())
                .availableQuantity(p.getAvailableQuantity()).imageUrl(p.getImageUrl())
                .brand(p.getBrand()).active(p.getActive()).taxRate(p.getTaxRate())
                .createdAt(p.getCreatedAt()).build();
    }

    private PageResponse<ProductResponse> toPage(Page<Product> page) {
        return PageResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .last(page.isLast()).build();
    }
}
