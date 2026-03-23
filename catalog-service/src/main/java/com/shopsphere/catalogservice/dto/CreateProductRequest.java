package com.shopsphere.catalogservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    private int stockQuantity;

    private String imageUrl;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private boolean featured;
}