package com.shopsphere.catalogservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEvent {

    private Long productId;
    private int quantity;
}