package com.shopsphere.orderservice.dto;

import com.shopsphere.common.enums.DeliveryMethod;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMode;
import com.shopsphere.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private DeliveryMethod deliveryMethod;
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private List<OrderItemDTO> items;
    private LocalDateTime placedAt;
    private LocalDateTime createdAt;
}