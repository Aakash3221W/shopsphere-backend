package com.shopsphere.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFailedEvent {
    private Long orderId;
    private Long userId;
    private String reason;
}