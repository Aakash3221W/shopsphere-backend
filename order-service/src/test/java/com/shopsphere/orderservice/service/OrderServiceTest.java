package com.shopsphere.orderservice.service;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentMode;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.orderservice.dto.*;
import com.shopsphere.orderservice.entity.*;
import com.shopsphere.orderservice.feign.CatalogFeignClient;
import com.shopsphere.orderservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Unit Tests")
class OrderServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private CatalogFeignClient catalogFeignClient;
    @Mock private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private Cart testCart;
    private CartItem testCartItem;
    private Order testOrder;
    private OrderItem testOrderItem;
    private ProductResponse testProduct;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        testProduct = new ProductResponse();
        testProduct.setId(1L);
        testProduct.setName("iPhone 15");
        testProduct.setPrice(new BigDecimal("79999.00"));
        testProduct.setStockQuantity(50);
        testProduct.setActive(true);

        testCartItem = CartItem.builder()
                .id(1L)
                .productId(1L)
                .productName("iPhone 15")
                .unitPrice(new BigDecimal("79999.00"))
                .quantity(2)
                .build();

        testCart = Cart.builder()
                .id(1L)
                .userId(1L)
                .items(new ArrayList<>(List.of(testCartItem)))
                .build();

        testCartItem.setCart(testCart);

        testOrderItem = OrderItem.builder()
                .id(1L)
                .productId(1L)
                .productName("iPhone 15")
                .unitPrice(new BigDecimal("79999.00"))
                .quantity(2)
                .lineTotal(new BigDecimal("159998.00"))
                .build();

        testOrder = Order.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.CHECKOUT)
                .totalAmount(new BigDecimal("159998.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .items(new ArrayList<>(List.of(testOrderItem)))
                .build();

        testOrderItem.setOrder(testOrder);

        paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentMode(PaymentMode.CARD);
    }

    // ─── Get Cart Tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Get Cart — creates new cart if none exists")
    void getCart_createsNewCartIfNotExists() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        ApiResponse<CartDTO> response = orderService.getCart(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Get Cart — returns existing cart")
    void getCart_returnsExistingCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        ApiResponse<CartDTO> response = orderService.getCart(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getUserId()).isEqualTo(1L);
        assertThat(response.getData().getTotalItems()).isEqualTo(1);
    }

    // ─── Add To Cart Tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("Add To Cart — success adds new item")
    void addToCart_success_newItem() {
        Cart emptyCart = Cart.builder()
                .id(1L)
                .userId(1L)
                .items(new ArrayList<>())
                .build();

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        ApiResponse<ProductResponse> productResponse =
                ApiResponse.success(testProduct);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));
        when(catalogFeignClient.getProductById(1L)).thenReturn(productResponse);
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        ApiResponse<CartDTO> response = orderService.addToCart(1L, request);

        assertThat(response.isSuccess()).isTrue();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Add To Cart — throws BadRequestException for insufficient stock")
    void addToCart_insufficientStock_throwsBadRequest() {
        testProduct.setStockQuantity(1);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(5);

        ApiResponse<ProductResponse> productResponse =
                ApiResponse.success(testProduct);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(catalogFeignClient.getProductById(1L)).thenReturn(productResponse);

        assertThatThrownBy(() -> orderService.addToCart(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Add To Cart — throws BadRequestException for inactive product")
    void addToCart_inactiveProduct_throwsBadRequest() {
        testProduct.setActive(false);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        ApiResponse<ProductResponse> productResponse =
                ApiResponse.success(testProduct);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(catalogFeignClient.getProductById(1L)).thenReturn(productResponse);

        assertThatThrownBy(() -> orderService.addToCart(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
    }

    // ─── Remove Cart Item Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("Remove Cart Item — success")
    void removeCartItem_success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        ApiResponse<CartDTO> response = orderService.removeCartItem(1L, 1L);

        assertThat(response.isSuccess()).isTrue();
        verify(cartRepository).save(any(Cart.class));
    }

    // ─── Clear Cart Tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Clear Cart — success clears all items")
    void clearCart_success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        ApiResponse<Void> response = orderService.clearCart(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(cartRepository).save(any(Cart.class));
    }

    // ─── Start Checkout Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Start Checkout — success creates order from cart")
    void startCheckout_success() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        ApiResponse<OrderDTO> response = orderService.startCheckout(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(OrderStatus.CHECKOUT);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Start Checkout — throws BadRequestException for empty cart")
    void startCheckout_emptyCart_throwsBadRequest() {
        Cart emptyCart = Cart.builder()
                .id(1L)
                .userId(1L)
                .items(new ArrayList<>())
                .build();

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> orderService.startCheckout(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    // ─── Process Payment Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Process Payment — success publishes order placed event")
    void processPayment_success() {
        when(orderRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        ApiResponse<OrderDTO> response =
                orderService.processPayment(1L, 1L, paymentRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderEventPublisher).publishOrderPlaced(any(Order.class));
    }

    @Test
    @DisplayName("Process Payment — throws BadRequestException for already paid order")
    void processPayment_alreadyPaid_throwsBadRequest() {
        testOrder.setStatus(OrderStatus.PAID);

        when(orderRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() ->
                orderService.processPayment(1L, 1L, paymentRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be modified");
    }

    // ─── Cancel Order Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("Cancel Order — success publishes order cancelled event")
    void cancelOrder_success() {
        when(orderRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        ApiResponse<OrderDTO> response = orderService.cancelOrder(1L, 1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventPublisher).publishOrderCancelled(any(Order.class));
    }

    @Test
    @DisplayName("Cancel Order — throws BadRequestException for delivered order")
    void cancelOrder_deliveredOrder_throwsBadRequest() {
        testOrder.setStatus(OrderStatus.DELIVERED);

        when(orderRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BadRequestException.class);
    }

    // ─── Get Order Tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Get Order By Id — success")
    void getOrderById_success() {
        when(orderRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testOrder));

        ApiResponse<OrderDTO> response = orderService.getOrderById(1L, 1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get Order By Id — throws ResourceNotFoundException")
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}