package com.shopsphere.orderservice.dto;

import com.shopsphere.common.enums.PaymentMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String transactionId;
}