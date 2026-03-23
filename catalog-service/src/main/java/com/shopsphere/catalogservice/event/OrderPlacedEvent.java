package com.shopsphere.catalogservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private Long orderId;
    private Long userId;
    private List<OrderItemEvent> items;
}