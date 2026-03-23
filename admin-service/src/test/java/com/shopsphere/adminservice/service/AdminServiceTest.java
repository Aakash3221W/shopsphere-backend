package com.shopsphere.adminservice.service;

import com.shopsphere.adminservice.dto.*;
import com.shopsphere.adminservice.feign.AuthFeignClient;
import com.shopsphere.adminservice.feign.CatalogFeignClient;
import com.shopsphere.adminservice.feign.OrderFeignClient;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Service Unit Tests")
class AdminServiceTest {

    @Mock private OrderFeignClient orderFeignClient;
    @Mock private CatalogFeignClient catalogFeignClient;
    @Mock private AuthFeignClient authFeignClient;

    @InjectMocks
    private AdminService adminService;

    private OrderResponse testOrder;
    private ProductResponse testProduct;
    private ProductRequest productRequest;
    private UserDTO testUser;
    private PagedResponse<OrderResponse> pagedOrders;
    private PagedResponse<UserDTO> pagedUsers;

    @BeforeEach
    void setUp() {
        testOrder = new OrderResponse();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setStatus(OrderStatus.PAID);
        testOrder.setTotalAmount(new BigDecimal("79999.00"));

        testProduct = new ProductResponse();
        testProduct.setId(1L);
        testProduct.setName("iPhone 15");
        testProduct.setPrice(new BigDecimal("79999.00"));

        productRequest = new ProductRequest();
        productRequest.setName("iPhone 15");
        productRequest.setPrice(new BigDecimal("79999.00"));
        productRequest.setCategoryId(1L);
        productRequest.setStockQuantity(50);

        testUser = new UserDTO();
        testUser.setId(1L);
        testUser.setEmail("test@shopsphere.com");
        testUser.setRole(Role.CUSTOMER);

        pagedOrders = new PagedResponse<>(
                List.of(testOrder), 0, 10, 1L, 1, true);

        pagedUsers = new PagedResponse<>(
                List.of(testUser), 0, 10, 1L, 1, true);
    }

    // ─── Dashboard Tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Get Dashboard — success returns metrics")
    void getDashboard_success() {
        when(orderFeignClient.getAllOrders(0, 1000))
                .thenReturn(ApiResponse.success(pagedOrders));
        when(authFeignClient.getAllUsers(0, 1))
                .thenReturn(ApiResponse.success(pagedUsers));

        ApiResponse<DashboardDTO> response = adminService.getDashboard();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getTotalOrders()).isEqualTo(1L);
        assertThat(response.getData().getTotalUsers()).isEqualTo(1L);
        assertThat(response.getData().getPendingOrders()).isEqualTo(1L);
        assertThat(response.getData().getTotalRevenue())
                .isEqualByComparingTo(new BigDecimal("79999.00"));
    }

    @Test
    @DisplayName("Get Dashboard — handles Auth Service failure gracefully")
    void getDashboard_authServiceDown_stillReturnsMetrics() {
        when(orderFeignClient.getAllOrders(0, 1000))
                .thenReturn(ApiResponse.success(pagedOrders));
        when(authFeignClient.getAllUsers(0, 1))
                .thenThrow(new RuntimeException("Auth Service down"));

        ApiResponse<DashboardDTO> response = adminService.getDashboard();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getTotalUsers()).isEqualTo(0L);
        assertThat(response.getData().getTotalOrders()).isEqualTo(1L);
    }

    // ─── Order Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Get All Orders — success")
    void getAllOrders_success() {
        when(orderFeignClient.getAllOrders(0, 10))
                .thenReturn(ApiResponse.success(pagedOrders));

        ApiResponse<PagedResponse<OrderResponse>> response =
                adminService.getAllOrders(0, 10);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getContent()).hasSize(1);
        assertThat(response.getData().getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Get Order By Id — success")
    void getOrderById_success() {
        when(orderFeignClient.getOrderById(1L))
                .thenReturn(ApiResponse.success(testOrder));

        ApiResponse<OrderResponse> response = adminService.getOrderById(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1L);
        assertThat(response.getData().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("Update Order Status — success")
    void updateOrderStatus_success() {
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderFeignClient.updateOrderStatus(1L, OrderStatus.SHIPPED))
                .thenReturn(ApiResponse.success(testOrder));

        ApiResponse<OrderResponse> response =
                adminService.updateOrderStatus(1L, OrderStatus.SHIPPED);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderFeignClient).updateOrderStatus(1L, OrderStatus.SHIPPED);
    }

    // ─── Product Tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Create Product — success")
    void createProduct_success() {
        when(catalogFeignClient.createProduct(any(ProductRequest.class)))
                .thenReturn(ApiResponse.success(testProduct));

        ApiResponse<ProductResponse> response =
                adminService.createProduct(productRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("iPhone 15");
        verify(catalogFeignClient).createProduct(any(ProductRequest.class));
    }

    @Test
    @DisplayName("Update Product — success")
    void updateProduct_success() {
        when(catalogFeignClient.updateProduct(eq(1L),
                any(ProductRequest.class)))
                .thenReturn(ApiResponse.success(testProduct));

        ApiResponse<ProductResponse> response =
                adminService.updateProduct(1L, productRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1L);
        verify(catalogFeignClient).updateProduct(eq(1L),
                any(ProductRequest.class));
    }

    @Test
    @DisplayName("Delete Product — success")
    void deleteProduct_success() {
        when(catalogFeignClient.deleteProduct(1L))
                .thenReturn(ApiResponse.success(null));

        ApiResponse<Void> response = adminService.deleteProduct(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(catalogFeignClient).deleteProduct(1L);
    }

    @Test
    @DisplayName("Update Stock — success")
    void updateStock_success() {
        testProduct.setStockQuantity(100);
        when(catalogFeignClient.updateStock(1L, 100))
                .thenReturn(ApiResponse.success(testProduct));

        ApiResponse<ProductResponse> response =
                adminService.updateStock(1L, 100);

        assertThat(response.isSuccess()).isTrue();
        verify(catalogFeignClient).updateStock(1L, 100);
    }

    // ─── User Tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Get All Users — success")
    void getAllUsers_success() {
        when(authFeignClient.getAllUsers(0, 10))
                .thenReturn(ApiResponse.success(pagedUsers));

        ApiResponse<PagedResponse<UserDTO>> response =
                adminService.getAllUsers(0, 10);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getContent()).hasSize(1);
        assertThat(response.getData().getContent().get(0).getEmail())
                .isEqualTo("test@shopsphere.com");
    }
}