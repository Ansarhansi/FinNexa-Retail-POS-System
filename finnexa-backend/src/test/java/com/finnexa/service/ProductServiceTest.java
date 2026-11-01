
package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.ProductRequest;
import com.finnexa.dto.ProductResponse;
import com.finnexa.entity.Product;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private ProductService productService;

	private Product product;
	private ProductRequest productRequest;

	@BeforeEach
	void setUp() {

		product = new Product();
		product.setId(1L);
		product.setName("Wireless Mouse");
		product.setSku("FNX-MOU-001");
		product.setDescription("Wireless optical mouse");
		product.setPrice(new BigDecimal("799.00"));
		product.setQuantity(50);

		productRequest = new ProductRequest();
		productRequest.setName("Wireless Mouse");
		productRequest.setSku("FNX-MOU-001");
		productRequest.setDescription("Wireless optical mouse");
		productRequest.setPrice(new BigDecimal("799.00"));
		productRequest.setQuantity(50);
	}

	// 1. Create product successfully
	@Test
	void createProductShouldReturnCreatedProduct() {

		when(productRepository.existsBySkuIgnoreCase("FNX-MOU-001")).thenReturn(false);

		when(productRepository.save(any(Product.class))).thenReturn(product);

		ProductResponse response = productService.createProduct(productRequest);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Wireless Mouse", response.getName());
		assertEquals("FNX-MOU-001", response.getSku());
		assertEquals(new BigDecimal("799.00"), response.getPrice());
		assertEquals(50, response.getQuantity());

		verify(productRepository).save(any(Product.class));

		verify(auditLogService).recordAction(eq("PRODUCT_CREATED"), eq("Product"), eq(1L), any(String.class));
	}

	// 2. Normalize SKU and description
	@Test
	void createProductShouldNormalizeSkuAndDescription() {

		productRequest.setSku("  fnx-mou-001  ");
		productRequest.setDescription("  Wireless mouse  ");

		when(productRepository.existsBySkuIgnoreCase("FNX-MOU-001")).thenReturn(false);

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		ProductResponse response = productService.createProduct(productRequest);

		assertEquals("FNX-MOU-001", response.getSku());
		assertEquals("Wireless mouse", response.getDescription());
	}

	// 3. Reject null creation request
	@Test
	void createProductShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> productService.createProduct(null));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 4. Reject empty SKU
	@Test
	void createProductShouldRejectEmptySku() {

		productRequest.setSku("  ");

		assertThrows(BadRequestException.class, () -> productService.createProduct(productRequest));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 5. Reject duplicate SKU
	@Test
	void createProductShouldRejectDuplicateSku() {

		when(productRepository.existsBySkuIgnoreCase("FNX-MOU-001")).thenReturn(true);

		assertThrows(BadRequestException.class, () -> productService.createProduct(productRequest));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 6. Reject empty product name
	@Test
	void createProductShouldRejectEmptyName() {

		productRequest.setName(" ");

		assertThrows(BadRequestException.class, () -> productService.createProduct(productRequest));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 7. Reject negative price
	@Test
	void createProductShouldRejectNegativePrice() {

		productRequest.setPrice(new BigDecimal("-10.00"));

		assertThrows(BadRequestException.class, () -> productService.createProduct(productRequest));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 8. Reject negative quantity
	@Test
	void createProductShouldRejectNegativeQuantity() {

		productRequest.setQuantity(-5);

		assertThrows(BadRequestException.class, () -> productService.createProduct(productRequest));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 9. Retrieve all products
	@Test
	void getAllProductsShouldReturnAllProducts() {

		when(productRepository.findAll()).thenReturn(List.of(product));

		List<ProductResponse> responses = productService.getAllProducts();

		assertEquals(1, responses.size());
		assertEquals("Wireless Mouse", responses.get(0).getName());

		verify(productRepository).findAll();
	}

	// 10. Retrieve product by ID
	@Test
	void getProductByIdShouldReturnProduct() {

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		ProductResponse response = productService.getProductById(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Wireless Mouse", response.getName());
	}

	// 11. Handle missing product ID
	@Test
	void getProductByIdShouldThrowWhenProductDoesNotExist() {

		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(999L));
	}

	// 12. Reject invalid product IDs
	@Test
	void getProductByIdShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> productService.getProductById(0L));

		assertThrows(BadRequestException.class, () -> productService.getProductById(-1L));

		assertThrows(BadRequestException.class, () -> productService.getProductById(null));
	}

	// 13. Retrieve product by normalized SKU
	@Test
	void getProductBySkuShouldReturnProduct() {

		when(productRepository.findBySkuIgnoreCase("FNX-MOU-001")).thenReturn(Optional.of(product));

		ProductResponse response = productService.getProductBySku(" fnx-mou-001 ");

		assertEquals("FNX-MOU-001", response.getSku());

		verify(productRepository).findBySkuIgnoreCase("FNX-MOU-001");
	}

	// 14. Handle missing SKU
	@Test
	void getProductBySkuShouldThrowWhenProductDoesNotExist() {

		when(productRepository.findBySkuIgnoreCase("UNKNOWN-SKU")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.getProductBySku("UNKNOWN-SKU"));
	}

	// 15. Reject empty SKU search
	@Test
	void getProductBySkuShouldRejectEmptySku() {

		assertThrows(BadRequestException.class, () -> productService.getProductBySku(" "));
	}

	// 16. Search products by name
	@Test
	void searchProductsShouldReturnMatchingProducts() {

		when(productRepository.findByNameContainingIgnoreCase("Mouse")).thenReturn(List.of(product));

		List<ProductResponse> responses = productService.searchProducts(" Mouse ");

		assertEquals(1, responses.size());
		assertEquals("Wireless Mouse", responses.get(0).getName());

		verify(productRepository).findByNameContainingIgnoreCase("Mouse");
	}

	// 17. Reject empty search
	@Test
	void searchProductsShouldRejectEmptyName() {

		assertThrows(BadRequestException.class, () -> productService.searchProducts(" "));

		assertThrows(BadRequestException.class, () -> productService.searchProducts(null));
	}

	// 18. Retrieve low-stock products
	@Test
	void getLowStockProductsShouldReturnProductsAtThreshold() {

		product.setQuantity(5);

		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of(product));

		List<ProductResponse> responses = productService.getLowStockProducts(5);

		assertEquals(1, responses.size());
		assertEquals(5, responses.get(0).getQuantity());
	}

	// 19. Reject negative stock threshold
	@Test
	void getLowStockProductsShouldRejectNegativeThreshold() {

		assertThrows(BadRequestException.class, () -> productService.getLowStockProducts(-1));
	}

	// 20. Reject null stock threshold
	@Test
	void getLowStockProductsShouldRejectNullThreshold() {

		assertThrows(BadRequestException.class, () -> productService.getLowStockProducts(null));
	}

	// 21. Update product successfully
	@Test
	void updateProductShouldUpdateExistingProduct() {

		productRequest.setName("Updated Wireless Mouse");
		productRequest.setPrice(new BigDecimal("899.00"));
		productRequest.setQuantity(40);

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService.updateProduct(1L, productRequest);

		assertEquals("Updated Wireless Mouse", response.getName());
		assertEquals(new BigDecimal("899.00"), response.getPrice());
		assertEquals(40, response.getQuantity());

		verify(auditLogService).recordAction(eq("PRODUCT_UPDATED"), eq("Product"), eq(1L), any(String.class));
	}

	// 22. Reject null update request
	@Test
	void updateProductShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> productService.updateProduct(1L, null));
	}

	// 23. Handle missing product during update
	@Test
	void updateProductShouldThrowWhenProductDoesNotExist() {

		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(999L, productRequest));
	}

	// 24. Reject duplicate SKU during update
	@Test
	void updateProductShouldRejectDuplicateSku() {

		productRequest.setSku("FNX-NEW-001");

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(productRepository.existsBySkuIgnoreCase("FNX-NEW-001")).thenReturn(true);

		assertThrows(BadRequestException.class, () -> productService.updateProduct(1L, productRequest));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 25. Allow retaining the existing SKU
	@Test
	void updateProductShouldAllowKeepingSameSku() {

		productRequest.setSku("fnx-mou-001");

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService.updateProduct(1L, productRequest);

		assertEquals("FNX-MOU-001", response.getSku());

		verify(productRepository, never()).existsBySkuIgnoreCase("FNX-MOU-001");
	}

	// 26. Reject empty SKU during update
	@Test
	void updateProductShouldRejectEmptySku() {

		productRequest.setSku(" ");

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> productService.updateProduct(1L, productRequest));

		verify(productRepository, never()).save(any(Product.class));
	}

	// 27. Delete product and record audit
	@Test
	void deleteProductShouldDeleteAndRecordAudit() {

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		productService.deleteProduct(1L);

		verify(auditLogService).recordAction(eq("PRODUCT_DELETED"), eq("Product"), eq(1L), any(String.class));

		verify(productRepository).delete(product);
	}

	// 28. Handle missing product during deletion
	@Test
	void deleteProductShouldThrowWhenProductDoesNotExist() {

		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(999L));

		verify(productRepository, never()).delete(any(Product.class));
	}

	// 29. Convert blank description to null
	@Test
	void createProductShouldConvertBlankDescriptionToNull() {

		productRequest.setDescription(" ");

		when(productRepository.existsBySkuIgnoreCase("FNX-MOU-001")).thenReturn(false);

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
			Product saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		ProductResponse response = productService.createProduct(productRequest);

		assertNull(response.getDescription());
	}

	// 30. Reject negative price during update
	@Test
	void updateProductShouldRejectNegativePrice() {

		productRequest.setPrice(new BigDecimal("-1.00"));

		assertThrows(BadRequestException.class, () -> productService.updateProduct(1L, productRequest));
	}

	// 31. Reject negative quantity during update
	@Test
	void updateProductShouldRejectNegativeQuantity() {

		productRequest.setQuantity(-1);

		assertThrows(BadRequestException.class, () -> productService.updateProduct(1L, productRequest));
	}
}
