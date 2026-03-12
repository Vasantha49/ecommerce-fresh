package com.ecommerce.dto;

import com.ecommerce.entity.Address;
import com.ecommerce.entity.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateOrderRequest {
    @NotNull private String sessionId;
    @NotNull @Valid private Address shippingAddress;
    @NotNull private Order.PaymentMethod paymentMethod;
    private String notes;
    private String cardNumber;
    private String cardExpiry;
    private String cardCvv;
}
