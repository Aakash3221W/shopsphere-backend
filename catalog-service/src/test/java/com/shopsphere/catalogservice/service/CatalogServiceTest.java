package com.shopsphere.catalogservice.service;

import com.shopsphere.catalogservice.dto.*;
import com.shopsphere.catalogservice.entity.Category;
import com.shopsphere.catalogservice.entity.Product;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ConflictException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catalog Service Unit Tests")
class CatalogServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private CatalogService catalogService;

    private Category testCategory;
    private Product testProduct;
    private CreateProductRequest createProductRequest;
    private CreateCategoryRequest createCategoryRequest;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic products")
                .isActive(true)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("iPhone 15")
                .description("Apple iPhone 15 128GB")
                .price(new BigDecimal("79999.00"))
                .originalPrice(new BigDecimal("89999.00"))
                .stockQuantity(50)
                .category(testCategory)
                .isActive(true)
                .isFeatured(true)
                .build();

        createProductRequest = new CreateProductRequest();
        createProductRequest.setName("iPhone 15");
        createProductRequest.setDescription("Apple iPhone 15 128GB");
        createProductRequest.setPrice(new BigDecimal("79999.00"));
        createProductRequest.setOriginalPrice(new BigDecimal("89999.00"));
        createProductRequest.setStockQuantity(50);
        createProductRequest.setCategoryId(1L);
        createProductRequest.setFeatured(true);

        createCategoryRequest = new CreateCategoryRequest();
        createCategoryRequest.setName("Electronics");
        createCategoryRequest.setDescription("Electronic products");
    }

    // ─── Get Product Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("Get Product By Id — success")
    void getProductById_success() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));

        ApiResponse<ProductDTO> response = catalogService.getProductById(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("iPhone 15");
        assertThat(response.getData().getPrice())
                .isEqualByComparingTo(new BigDecimal("79999.00"));
    }

    @Test
    @DisplayName("Get Product By Id — throws ResourceNotFoundException")
    void getProductById_notFound_throwsException() {
        when(productRepository.findByIdAndIsActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Get Featured Products Tests ─────────────────────────────────────────

    @Test
    @DisplayName("Get Featured Products — returns featured list")
    void getFeaturedProducts_success() {
        when(productRepository.findByIsFeaturedTrueAndIsActiveTrue())
                .thenReturn(List.of(testProduct));

        ApiResponse<List<ProductDTO>> response =
                catalogService.getFeaturedProducts();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getName()).isEqualTo("iPhone 15");
    }

    // ─── Create Product Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Create Product — success")
    void createProduct_success() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        ApiResponse<ProductDTO> response =
                catalogService.createProduct(createProductRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("iPhone 15");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Create Product — throws ResourceNotFoundException for invalid category")
    void createProduct_invalidCategory_throwsException() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                catalogService.createProduct(createProductRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    // ─── Delete Product Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Delete Product — soft deletes product")
    void deleteProduct_success() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        ApiResponse<Void> response = catalogService.deleteProduct(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(productRepository).save(any(Product.class));
        assertThat(testProduct.isActive()).isFalse();
    }

    @Test
    @DisplayName("Delete Product — throws ResourceNotFoundException")
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findByIdAndIsActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    // ─── Update Stock Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Update Stock — success")
    void updateStock_success() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        ApiResponse<ProductDTO> response = catalogService.updateStock(1L, 100);

        assertThat(response.isSuccess()).isTrue();
        assertThat(testProduct.getStockQuantity()).isEqualTo(100);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Update Stock — throws BadRequestException for negative quantity")
    void updateStock_negativeQuantity_throwsBadRequest() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> catalogService.updateStock(1L, -5))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negative");
    }

    // ─── Category Tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Create Category — success")
    void createCategory_success() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(testCategory);

        ApiResponse<CategoryDTO> response =
                catalogService.createCategory(createCategoryRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Create Category — throws ConflictException for duplicate name")
    void createCategory_duplicateName_throwsConflict() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                catalogService.createCategory(createCategoryRequest))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any());
    }

    // ─── Get All Categories Tests ────────────────────────────────────────────

    @Test
    @DisplayName("Get All Categories — returns active categories")
    void getAllCategories_success() {
        when(categoryRepository.findByIsActiveTrue())
                .thenReturn(List.of(testCategory));

        ApiResponse<List<CategoryDTO>> response =
                catalogService.getAllCategories();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getName()).isEqualTo("Electronics");
    }

    // ─── Get Products By Category Tests ──────────────────────────────────────

    @Test
    @DisplayName("Get Products By Category — throws ResourceNotFoundException for invalid category")
    void getProductsByCategory_invalidCategory_throwsException() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() ->
                catalogService.getProductsByCategory(99L, 0, 12))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get Products By Category — success")
    void getProductsByCategory_success() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByCategoryIdAndIsActiveTrue(
                eq(1L), any(Pageable.class))).thenReturn(page);

        var response = catalogService.getProductsByCategory(1L, 0, 12);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getContent()).hasSize(1);
    }

    // ─── Update Product Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Update Product — throws ResourceNotFoundException for product not found")
    void updateProduct_productNotFound_throwsException() {
        when(productRepository.findByIdAndIsActiveTrue(99L))
                .thenReturn(Optional.empty());

        CreateProductRequest request = new CreateProductRequest();
        request.setName("Updated Product");
        request.setCategoryId(1L);

        assertThatThrownBy(() -> catalogService.updateProduct(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    @DisplayName("Update Product — throws ResourceNotFoundException for invalid category")
    void updateProduct_invalidCategory_throwsException() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        CreateProductRequest request = new CreateProductRequest();
        request.setName("Updated Product");
        request.setCategoryId(99L);

        assertThatThrownBy(() -> catalogService.updateProduct(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("Update Product — success")
    void updateProduct_success() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        CreateProductRequest request = new CreateProductRequest();
        request.setName("Updated Product");
        request.setDescription("Updated description");
        request.setPrice(new BigDecimal("89999.00"));
        request.setCategoryId(1L);

        ApiResponse<ProductDTO> response = catalogService.updateProduct(1L, request);

        assertThat(response.isSuccess()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    // ─── Update Category Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Update Category — throws ResourceNotFoundException for category not found")
    void updateCategory_notFound_throwsException() {
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Updated Category");

        assertThatThrownBy(() -> catalogService.updateCategory(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("Update Category — success")
    void updateCategory_success() {
        Category updatedCategory = Category.builder()
                .id(1L)
                .name("Updated Electronics")
                .description("Updated description")
                .isActive(true)
                .build();

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(updatedCategory);

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Updated Electronics");
        request.setDescription("Updated description");

        ApiResponse<CategoryDTO> response = catalogService.updateCategory(1L, request);

        assertThat(response.isSuccess()).isTrue();
        verify(categoryRepository).save(any(Category.class));
    }

    // ─── Pagination Edge Case Tests ───────────────────────────────────────────

    @Test
    @DisplayName("Get All Products — size exceeds MAX_PAGE_SIZE is capped")
    void getAllProducts_sizeExceedsMaxPageSize_isCapped() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(productRepository.searchProducts(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        catalogService.getAllProducts(null, null, null, null, null, 0, 100);

        verify(productRepository).searchProducts(
                any(), any(), any(), any(),
                argThat(pageable -> pageable.getPageSize() == 50));
    }

    // ─── Get All Products with Sorting Tests ──────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "'price_asc',  false",
        "'price_desc', false",
        "'name_asc',   false",
        "'newest',     false",
        "'unknown',    false",
        "'',          false",
        "null,        true"
    })
    @DisplayName("Get All Products — with sort option {0}")
    void getAllProducts_withSortOptions(String sort, boolean isNull) {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(productRepository.searchProducts(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        String sortValue = isNull ? null : sort;
        var response = catalogService.getAllProducts(null, null, null, null, sortValue, 0, 12);

        assertThat(response.isSuccess()).isTrue();
    }

    // ─── Product with Null Category Tests ─────────────────────────────────────

    @Test
    @DisplayName("Get Product By Id — product with null category")
    void getProductById_nullCategory_handlesGracefully() {
        Product productWithNullCategory = Product.builder()
                .id(2L)
                .name("Standalone Product")
                .category(null)
                .isActive(true)
                .build();

        when(productRepository.findByIdAndIsActiveTrue(2L))
                .thenReturn(Optional.of(productWithNullCategory));

        ApiResponse<ProductDTO> response = catalogService.getProductById(2L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getCategoryId()).isNull();
        assertThat(response.getData().getCategoryName()).isNull();
    }
}