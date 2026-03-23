package com.shopsphere.adminservice.dto;

import com.shopsphere.common.enums.DeliveryMethod;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMode;
import com.shopsphere.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private DeliveryMethod deliveryMethod;
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private LocalDateTime placedAt;
    private LocalDateTime createdAt;
}