package com.shopsphere.orderservice.dto;

import com.shopsphere.common.enums.DeliveryMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotNull(message = "Delivery method is required")
    private DeliveryMethod deliveryMethod;
}