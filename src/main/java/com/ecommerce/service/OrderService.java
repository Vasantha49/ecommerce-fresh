package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.exception.PaymentException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final PaymentService paymentService;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal SHIPPING_COST           = new BigDecimal("4.99");

    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        Cart cart = cartService.getCartEntity(req.getSessionId());
        if (cart.getItems().isEmpty()) throw new RuntimeException("Cart is empty");

        // Reserve stock atomically
        for (CartItem item : cart.getItems()) {
            if (!productService.reserveStock(item.getProduct().getId(), item.getQuantity())) {
                // Release already-reserved items on failure
                cart.getItems().stream()
                        .filter(i -> !i.getProduct().getId().equals(item.getProduct().getId()))
                        .forEach(i -> productService.releaseReservedStock(i.getProduct().getId(), i.getQuantity()));
                throw new RuntimeException("Insufficient stock for: " + item.getProduct().getName());
            }
        }

        // Build order
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        Order order = Order.builder().userId(userId)
                .shippingAddress(req.getShippingAddress())
                .paymentMethod(req.getPaymentMethod())
                .notes(req.getNotes()).build();

        for (CartItem ci : cart.getItems()) {
            Product p = ci.getProduct();
            BigDecimal lineTotal = ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            BigDecimal lineTax   = lineTotal.multiply(p.getTaxRate()).divide(BigDecimal.valueOf(100));
            subtotal  = subtotal.add(lineTotal);
            taxAmount = taxAmount.add(lineTax);
            order.getItems().add(OrderItem.builder().order(order).product(p)
                    .quantity(ci.getQuantity()).unitPrice(ci.getUnitPrice())
                    .taxRate(p.getTaxRate()).subtotal(lineTotal)
                    .productName(p.getName()).productSku(p.getSku()).build());
        }

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_COST;
        BigDecimal total    = subtotal.add(taxAmount).add(shipping);
        order.setSubtotal(subtotal); order.setTaxAmount(taxAmount);
        order.setShippingCost(shipping); order.setTotalAmount(total);
        Order saved = orderRepository.save(order);

        // Process payment
        PaymentService.PaymentResult result = paymentService.processPayment(
                saved.getOrderNumber(), total, req.getPaymentMethod().name(), req.getCardNumber());

        if (result.success()) {
            cart.getItems().forEach(i -> productService.confirmStockDeduction(i.getProduct().getId(), i.getQuantity()));
            saved.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            saved.setPaymentTransactionId(result.transactionId());
            saved.setStatus(Order.OrderStatus.CONFIRMED);
            cartService.clearCart(req.getSessionId());
            log.info("Order {} created — total: {} EUR", saved.getOrderNumber(), total);
        } else {
            cart.getItems().forEach(i -> productService.releaseReservedStock(i.getProduct().getId(), i.getQuantity()));
            saved.setPaymentStatus(Order.PaymentStatus.FAILED);
            saved.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(saved);
            throw new PaymentException("Payment failed: " + result.errorMessage());
        }

        return toResponse(orderRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return toResponse(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id)));
    }

    @Transactional(readOnly = true)
    public OrderResponse getByNumber(String orderNumber) {
        return toResponse(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber)));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Long userId, int page, int size) {
        Page<Order> p = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PageResponse.<OrderResponse>builder()
                .content(p.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(p.getNumber()).size(p.getSize()).totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages()).last(p.isLast()).build();
    }

    public OrderResponse updateStatus(Long id, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        if (newStatus == Order.OrderStatus.SHIPPED)    order.setShippedAt(LocalDateTime.now());
        if (newStatus == Order.OrderStatus.DELIVERED)  order.setDeliveredAt(LocalDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    public OrderResponse cancel(Long id, Long userId) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
        if (!order.getUserId().equals(userId)) throw new RuntimeException("Access denied");
        if (!List.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED).contains(order.getStatus()))
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        order.getItems().forEach(i -> productService.releaseReservedStock(i.getProduct().getId(), i.getQuantity()));
        if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED) {
            paymentService.refundPayment(order.getPaymentTransactionId(), order.getTotalAmount());
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void cancelStaleOrders() {
        List<Order> stale = orderRepository.findStaleOrders(LocalDateTime.now().minusMinutes(30));
        stale.forEach(o -> {
            o.setStatus(Order.OrderStatus.CANCELLED);
            o.setPaymentStatus(Order.PaymentStatus.FAILED);
            o.getItems().forEach(i -> productService.releaseReservedStock(i.getProduct().getId(), i.getQuantity()));
            log.info("Auto-cancelled stale order: {}", o.getOrderNumber());
        });
        if (!stale.isEmpty()) orderRepository.saveAll(stale);
    }

    private void validateTransition(Order.OrderStatus from, Order.OrderStatus to) {
        boolean valid = switch (from) {
            case PENDING   -> to == Order.OrderStatus.CONFIRMED  || to == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> to == Order.OrderStatus.PROCESSING || to == Order.OrderStatus.CANCELLED;
            case PROCESSING -> to == Order.OrderStatus.SHIPPED;
            case SHIPPED   -> to == Order.OrderStatus.DELIVERED;
            default -> false;
        };
        if (!valid) throw new RuntimeException("Invalid status transition: " + from + " → " + to);
    }

    public OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId()).orderNumber(o.getOrderNumber()).userId(o.getUserId()).status(o.getStatus())
                .items(o.getItems().stream().map(i -> OrderItemResponse.builder()
                        .productId(i.getProduct().getId()).productName(i.getProductName())
                        .productSku(i.getProductSku()).quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice()).taxRate(i.getTaxRate()).subtotal(i.getSubtotal())
                        .build()).collect(Collectors.toList()))
                .subtotal(o.getSubtotal()).taxAmount(o.getTaxAmount())
                .shippingCost(o.getShippingCost()).totalAmount(o.getTotalAmount())
                .shippingAddress(o.getShippingAddress()).paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus()).paymentTransactionId(o.getPaymentTransactionId())
                .notes(o.getNotes()).shippedAt(o.getShippedAt()).deliveredAt(o.getDeliveredAt())
                .createdAt(o.getCreatedAt()).build();
    }
}
