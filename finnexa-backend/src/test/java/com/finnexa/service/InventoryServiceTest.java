package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.InventoryResponse;
import com.finnexa.entity.Product;
import com.finnexa.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private InventoryService inventoryService;

	private Product product;

	@BeforeEach
	void setUp() {
		product = new Product();
		product.setId(1L);
		product.setName("Keyboard");
		product.setSku("KEY-001");
		product.setPrice(new BigDecimal("800.00"));
		product.setQuantity(10);
	}

	private Product createProduct(Long id, String name, String sku, BigDecimal price, Integer quantity) {

		Product newProduct = new Product();
		newProduct.setId(id);
		newProduct.setName(name);
		newProduct.setSku(sku);
		newProduct.setPrice(price);
		newProduct.setQuantity(quantity);

		return newProduct;
	}

// =========================================================
// 1. GET ALL INVENTORY TESTS
// =========================================================

	@Test
	void getAllInventoryShouldReturnAllProducts() {
		Product secondProduct = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 20);

		when(productRepository.findAll()).thenReturn(List.of(product, secondProduct));

		List<InventoryResponse> responses = inventoryService.getAllInventory();

		assertNotNull(responses);
		assertEquals(2, responses.size());
		assertEquals("Keyboard", responses.get(0).getProductName());
		assertEquals("Mouse", responses.get(1).getProductName());

		verify(productRepository).findAll();
	}

	@Test
	void getAllInventoryShouldReturnEmptyList() {
		when(productRepository.findAll()).thenReturn(List.of());

		List<InventoryResponse> responses = inventoryService.getAllInventory();

		assertNotNull(responses);
		assertTrue(responses.isEmpty());
	}

	@Test
	void getAllInventoryShouldMapProductIdCorrectly() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertEquals(1L, response.getProductId());
	}

	@Test
	void getAllInventoryShouldMapProductNameCorrectly() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertEquals("Keyboard", response.getProductName());
	}

	@Test
	void getAllInventoryShouldMapSkuCorrectly() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertEquals("KEY-001", response.getSku());
	}

	@Test
	void getAllInventoryShouldMapQuantityCorrectly() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertEquals(10, response.getAvailableQuantity());
	}

	@Test
	void getAllInventoryShouldMapPriceCorrectly() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertEquals(new BigDecimal("800.00"), response.getUnitPrice());
	}

// =========================================================
// 2. GET INVENTORY BY PRODUCT ID TESTS
// =========================================================

	@Test
	void getInventoryByProductIdShouldReturnProduct() {
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		InventoryResponse response = inventoryService.getInventoryByProductId(1L);

		assertNotNull(response);
		assertEquals(1L, response.getProductId());
		assertEquals("Keyboard", response.getProductName());
		assertEquals("KEY-001", response.getSku());
		assertEquals(10, response.getAvailableQuantity());
		assertEquals(new BigDecimal("800.00"), response.getUnitPrice());
	}

	@Test
	void getInventoryByProductIdShouldRejectMissingProduct() {
		when(productRepository.findById(999L)).thenReturn(Optional.empty());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> inventoryService.getInventoryByProductId(999L));

		assertEquals("Product not found with ID: 999", exception.getMessage());
	}

	@Test
	void getInventoryByProductIdShouldPassIdToRepository() {
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		inventoryService.getInventoryByProductId(1L);

		verify(productRepository).findById(1L);
	}

	@Test
	void getInventoryByProductIdShouldHandleZeroIdAsMissingProduct() {
		when(productRepository.findById(0L)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> inventoryService.getInventoryByProductId(0L));
	}

	@Test
	void getInventoryByProductIdShouldHandleNegativeIdAsMissingProduct() {
		when(productRepository.findById(-1L)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> inventoryService.getInventoryByProductId(-1L));
	}

	@Test
	void getInventoryByProductIdShouldPassNullIdToRepository() {
		when(productRepository.findById(null)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> inventoryService.getInventoryByProductId(null));

		verify(productRepository).findById(null);
	}

// =========================================================
// 3. LOW-STOCK INVENTORY TESTS
// =========================================================

	@Test
	void getLowStockInventoryShouldReturnProductsAtThreshold() {
		Product lowStockProduct = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 5);

		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of(lowStockProduct));

		List<InventoryResponse> responses = inventoryService.getLowStockInventory();

		assertEquals(1, responses.size());
		assertEquals(5, responses.get(0).getAvailableQuantity());
		assertTrue(responses.get(0).isLowStock());
	}

	@Test
	void getLowStockInventoryShouldReturnProductsBelowThreshold() {
		Product lowStockProduct = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 3);

		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of(lowStockProduct));

		List<InventoryResponse> responses = inventoryService.getLowStockInventory();

		assertEquals(1, responses.size());
		assertEquals(3, responses.get(0).getAvailableQuantity());
		assertTrue(responses.get(0).isLowStock());
	}

	@Test
	void getLowStockInventoryShouldReturnOutOfStockProducts() {
		Product outOfStockProduct = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 0);

		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of(outOfStockProduct));

		List<InventoryResponse> responses = inventoryService.getLowStockInventory();

		assertEquals(1, responses.size());
		assertEquals(0, responses.get(0).getAvailableQuantity());
		assertTrue(responses.get(0).isLowStock());
	}

	@Test
	void getLowStockInventoryShouldReturnEmptyListWhenNoProductsMatch() {
		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of());

		List<InventoryResponse> responses = inventoryService.getLowStockInventory();

		assertNotNull(responses);
		assertTrue(responses.isEmpty());
	}

	@Test
	void getLowStockInventoryShouldUseThresholdOfFive() {
		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of());

		inventoryService.getLowStockInventory();

		verify(productRepository).findByQuantityLessThanEqual(5);
	}

	@Test
	void getLowStockInventoryShouldMarkNormalStockAsNotLowStock() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertFalse(response.isLowStock());
	}

	@Test
	void getLowStockInventoryShouldMarkQuantityFiveAsLowStock() {
		product.setQuantity(5);
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertTrue(response.isLowStock());
	}

	@Test
	void getLowStockInventoryShouldMarkQuantityZeroAsLowStock() {
		product.setQuantity(0);
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertTrue(response.isLowStock());
	}

// =========================================================
// 4. TOTAL PRODUCT COUNT TESTS
// =========================================================

	@Test
	void getTotalProductCountShouldReturnCount() {
		when(productRepository.count()).thenReturn(10L);

		long count = inventoryService.getTotalProductCount();

		assertEquals(10L, count);
	}

	@Test
	void getTotalProductCountShouldReturnZeroWhenEmpty() {
		when(productRepository.count()).thenReturn(0L);

		long count = inventoryService.getTotalProductCount();

		assertEquals(0L, count);
	}

	@Test
	void getTotalProductCountShouldCallRepositoryOnce() {
		when(productRepository.count()).thenReturn(5L);

		inventoryService.getTotalProductCount();

		verify(productRepository, times(1)).count();
	}

// =========================================================
// 5. LOW-STOCK PRODUCT COUNT TESTS
// =========================================================

	@Test
	void getLowStockProductCountShouldReturnCount() {
		Product first = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 3);

		Product second = createProduct(3L, "Monitor", "MON-001", new BigDecimal("5000.00"), 0);

		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of(first, second));

		long count = inventoryService.getLowStockProductCount();

		assertEquals(2L, count);
	}

	@Test
	void getLowStockProductCountShouldReturnZeroWhenNoLowStockProducts() {
		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of());

		long count = inventoryService.getLowStockProductCount();

		assertEquals(0L, count);
	}

	@Test
	void getLowStockProductCountShouldUseThresholdOfFive() {
		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of());

		inventoryService.getLowStockProductCount();

		verify(productRepository).findByQuantityLessThanEqual(5);
	}

// =========================================================
// 6. INVENTORY SELLING VALUE TESTS
// =========================================================

	@Test
	void getTotalInventorySellingValueShouldCalculateValue() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		BigDecimal value = inventoryService.getTotalInventorySellingValue();

		assertEquals(new BigDecimal("8000.00"), value);
	}

	@Test
	void getTotalInventorySellingValueShouldCalculateMultipleProducts() {
		Product secondProduct = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 20);

		when(productRepository.findAll()).thenReturn(List.of(product, secondProduct));

		BigDecimal value = inventoryService.getTotalInventorySellingValue();

		assertEquals(new BigDecimal("18000.00"), value);
	}

	@Test
	void getTotalInventorySellingValueShouldReturnZeroForEmptyInventory() {
		when(productRepository.findAll()).thenReturn(List.of());

		BigDecimal value = inventoryService.getTotalInventorySellingValue();

		assertEquals(BigDecimal.ZERO, value);
	}

	@Test
	void getTotalInventorySellingValueShouldReturnZeroForZeroQuantity() {
		product.setQuantity(0);
		when(productRepository.findAll()).thenReturn(List.of(product));

		BigDecimal value = inventoryService.getTotalInventorySellingValue();

		assertEquals(new BigDecimal("0.00"), value);
	}

	@Test
	void getTotalInventorySellingValueShouldHandleDifferentPrices() {
		Product first = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("125.50"), 4);

		Product second = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("75.25"), 2);

		when(productRepository.findAll()).thenReturn(List.of(first, second));

		BigDecimal value = inventoryService.getTotalInventorySellingValue();

		assertEquals(new BigDecimal("652.50"), value);

	}

	@Test
	void getTotalInventorySellingValueShouldUseSellingPriceAndQuantity() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		inventoryService.getTotalInventorySellingValue();

		verify(productRepository).findAll();
	}

// =========================================================
// 7. INVENTORY RESPONSE MAPPING TESTS
// =========================================================

	@Test
	void inventoryResponseShouldContainCorrectProductInformation() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertAll(() -> assertEquals(1L, response.getProductId()),
				() -> assertEquals("Keyboard", response.getProductName()),
				() -> assertEquals("KEY-001", response.getSku()),
				() -> assertEquals(10, response.getAvailableQuantity()),
				() -> assertEquals(new BigDecimal("800.00"), response.getUnitPrice()),
				() -> assertFalse(response.isLowStock()));
	}

	@Test
	void inventoryResponseShouldMarkQuantityFourAsLowStock() {
		product.setQuantity(4);
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertTrue(response.isLowStock());
	}

	@Test
	void inventoryResponseShouldMarkQuantitySixAsNotLowStock() {
		product.setQuantity(6);
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertFalse(response.isLowStock());
	}

	@Test
	void inventoryResponseShouldPreserveDecimalPrice() {
		product.setPrice(new BigDecimal("123.45"));
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventoryResponse response = inventoryService.getAllInventory().get(0);

		assertEquals(new BigDecimal("123.45"), response.getUnitPrice());
	}

}
