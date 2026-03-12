package com.ecommerce.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PaymentService {

    @Value("${app.payment.success-rate:0.95}")
    private double successRate;

    @Value("${app.payment.processing-delay-ms:500}")
    private long processingDelayMs;

    private final Random random = new Random();

    public PaymentResult processPayment(String orderId, BigDecimal amount,
                                        String method, String cardNumber) {
        log.info("Processing payment for order {} — {} EUR via {}", orderId, amount, method);
        try { TimeUnit.MILLISECONDS.sleep(processingDelayMs); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Simulate specific card failure scenarios
        if (cardNumber != null) {
            if (cardNumber.startsWith("0000"))
                return PaymentResult.failed("CARD_DECLINED", "Card declined by issuer");
            if (cardNumber.startsWith("9999"))
                return PaymentResult.failed("INSUFFICIENT_FUNDS", "Insufficient funds");
        }

        if (random.nextDouble() < successRate) {
            String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            log.info("Payment successful — txn: {}", txnId);
            return PaymentResult.success(txnId);
        }
        log.warn("Payment failed for order {}", orderId);
        return PaymentResult.failed("GATEWAY_ERROR", "Payment gateway temporarily unavailable");
    }

    public PaymentResult refundPayment(String transactionId, BigDecimal amount) {
        log.info("Refunding txn {} — {} EUR", transactionId, amount);
        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        return PaymentResult.success(refundId);
    }

    public record PaymentResult(boolean success, String transactionId, String errorCode, String errorMessage) {
        public static PaymentResult success(String txnId) {
            return new PaymentResult(true, txnId, null, null);
        }
        public static PaymentResult failed(String code, String message) {
            return new PaymentResult(false, null, code, message);
        }
    }
}
