package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.InventorySummaryResponse;
import com.finnexa.entity.Product;
import com.finnexa.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class InventorySummaryServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private InventorySummaryService inventorySummaryService;

	private Product product;

	@BeforeEach
	void setUp() {
		product = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("800.00"), 10);
	}

	private Product createProduct(Long id, String name, String sku, BigDecimal price, Integer quantity) {

		Product product = new Product();
		product.setId(id);
		product.setName(name);
		product.setSku(sku);
		product.setPrice(price);
		product.setQuantity(quantity);

		return product;
	}

// =========================================================
// 1. BASIC INVENTORY SUMMARY TESTS
// =========================================================

	@Test
	void getInventorySummaryShouldReturnCorrectSummary() {
		Product keyboard = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("800.00"), 10);

		Product mouse = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 3);

		Product monitor = createProduct(3L, "Monitor", "MON-001", new BigDecimal("5000.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(keyboard, mouse, monitor));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertNotNull(response);
		assertEquals(3L, response.getTotalProducts());
		assertEquals(1L, response.getLowStockProducts());
		assertEquals(1L, response.getOutOfStockProducts());
		assertEquals(new BigDecimal("9500.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldReturnZeroCountsForEmptyInventory() {
		when(productRepository.findAll()).thenReturn(List.of());

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertNotNull(response);
		assertEquals(0L, response.getTotalProducts());
		assertEquals(0L, response.getLowStockProducts());
		assertEquals(0L, response.getOutOfStockProducts());
		assertEquals(BigDecimal.ZERO, response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldCallRepositoryOnce() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		inventorySummaryService.getInventorySummary();

		verify(productRepository, times(1)).findAll();
	}

// =========================================================
// 2. TOTAL PRODUCT COUNT TESTS
// =========================================================

	@Test
	void getInventorySummaryShouldCountAllProducts() {
		Product second = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 20);

		Product third = createProduct(3L, "Monitor", "MON-001", new BigDecimal("5000.00"), 15);

		when(productRepository.findAll()).thenReturn(List.of(product, second, third));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(3L, response.getTotalProducts());
	}

	@Test
	void getInventorySummaryShouldCountProductsRegardlessOfStock() {
		Product normal = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 20);

		Product low = createProduct(3L, "Monitor", "MON-001", new BigDecimal("5000.00"), 2);

		Product out = createProduct(4L, "Webcam", "WEB-001", new BigDecimal("1500.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(normal, low, out));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(3L, response.getTotalProducts());
	}

// =========================================================
// 3. LOW-STOCK COUNT TESTS
// =========================================================

	@Test
	void getInventorySummaryShouldCountProductsWithQuantityFive() {
		Product lowStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 5);

		when(productRepository.findAll()).thenReturn(List.of(product, lowStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(1L, response.getLowStockProducts());
	}

	@Test
	void getInventorySummaryShouldCountProductsBelowFive() {
		Product lowStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 2);

		when(productRepository.findAll()).thenReturn(List.of(product, lowStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(1L, response.getLowStockProducts());
	}

	@Test
	void getInventorySummaryShouldExcludeProductsAboveFiveFromLowStock() {
		Product normalStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 6);

		when(productRepository.findAll()).thenReturn(List.of(product, normalStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(0L, response.getLowStockProducts());
	}

	@Test
	void getInventorySummaryShouldExcludeOutOfStockProductsFromLowStock() {
		Product outOfStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(product, outOfStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(0L, response.getLowStockProducts());
	}

	@Test
	void getInventorySummaryShouldCountMultipleLowStockProducts() {
		Product first = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 1);

		Product second = createProduct(3L, "Webcam", "WEB-001", new BigDecimal("1500.00"), 4);

		Product third = createProduct(4L, "Headphones", "HEAD-001", new BigDecimal("1000.00"), 5);

		when(productRepository.findAll()).thenReturn(List.of(product, first, second, third));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(3L, response.getLowStockProducts());
	}

	@Test
	void getInventorySummaryShouldReturnZeroLowStockCountWhenAllProductsAreInStock() {
		Product second = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 20);

		when(productRepository.findAll()).thenReturn(List.of(product, second));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(0L, response.getLowStockProducts());
	}

// =========================================================
// 4. OUT-OF-STOCK COUNT TESTS
// =========================================================

	@Test
	void getInventorySummaryShouldCountOutOfStockProducts() {
		Product outOfStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(product, outOfStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(1L, response.getOutOfStockProducts());
	}

	@Test
	void getInventorySummaryShouldCountMultipleOutOfStockProducts() {
		Product first = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 0);

		Product second = createProduct(3L, "Webcam", "WEB-001", new BigDecimal("1500.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(product, first, second));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(2L, response.getOutOfStockProducts());
	}

	@Test
	void getInventorySummaryShouldNotCountAvailableProductsAsOutOfStock() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(0L, response.getOutOfStockProducts());
	}

	@Test
	void getInventorySummaryShouldNotCountLowStockProductsAsOutOfStock() {
		Product lowStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 1);

		when(productRepository.findAll()).thenReturn(List.of(lowStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(0L, response.getOutOfStockProducts());
		assertEquals(1L, response.getLowStockProducts());
	}

// =========================================================
// 5. INVENTORY SELLING VALUE TESTS
// =========================================================

	@Test
	void getInventorySummaryShouldCalculateSellingValueForOneProduct() {
		when(productRepository.findAll()).thenReturn(List.of(product));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(new BigDecimal("8000.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldCalculateSellingValueForMultipleProducts() {
		Product keyboard = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("800.00"), 10);

		Product mouse = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 20);

		Product monitor = createProduct(3L, "Monitor", "MON-001", new BigDecimal("5000.00"), 2);

		when(productRepository.findAll()).thenReturn(List.of(keyboard, mouse, monitor));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		// Keyboard: 800 * 10 = 8,000
		// Mouse: 500 * 20 = 10,000
		// Monitor: 5000 * 2 = 10,000
		// Total: 28,000

		assertEquals(new BigDecimal("28000.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldReturnZeroValueForEmptyInventory() {
		when(productRepository.findAll()).thenReturn(List.of());

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(BigDecimal.ZERO, response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldReturnZeroValueForZeroQuantity() {
		Product outOfStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(outOfStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(new BigDecimal("0.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldCalculateDecimalSellingValues() {
		Product first = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("125.50"), 4);

		Product second = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("75.25"), 2);

		when(productRepository.findAll()).thenReturn(List.of(first, second));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(new BigDecimal("652.50"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldIncludeLowStockProductsInSellingValue() {
		Product lowStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 2);

		when(productRepository.findAll()).thenReturn(List.of(lowStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(new BigDecimal("1000.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldNotAddValueForOutOfStockProducts() {
		Product outOfStock = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(outOfStock));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(new BigDecimal("0.00"), response.getTotalInventorySellingValue());
	}

// =========================================================
// 6. COMBINED INVENTORY SCENARIOS
// =========================================================

	@Test
	void getInventorySummaryShouldHandleMixedStockLevels() {
		Product normal = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("800.00"), 10);

		Product low = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 3);

		Product out = createProduct(3L, "Monitor", "MON-001", new BigDecimal("5000.00"), 0);

		Product boundary = createProduct(4L, "Webcam", "WEB-001", new BigDecimal("1500.00"), 5);

		when(productRepository.findAll()).thenReturn(List.of(normal, low, out, boundary));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(4L, response.getTotalProducts());
		assertEquals(2L, response.getLowStockProducts());
		assertEquals(1L, response.getOutOfStockProducts());

		// 8000 + 1500 + 0 + 7500 = 17000
		assertEquals(new BigDecimal("17000.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldHandleOneProductAtLowStockBoundary() {
		Product boundary = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("800.00"), 5);

		when(productRepository.findAll()).thenReturn(List.of(boundary));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(1L, response.getTotalProducts());
		assertEquals(1L, response.getLowStockProducts());
		assertEquals(0L, response.getOutOfStockProducts());
		assertEquals(new BigDecimal("4000.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldHandleAllProductsOutOfStock() {
		Product first = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("800.00"), 0);

		Product second = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 0);

		when(productRepository.findAll()).thenReturn(List.of(first, second));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(2L, response.getTotalProducts());
		assertEquals(0L, response.getLowStockProducts());
		assertEquals(2L, response.getOutOfStockProducts());
		assertEquals(new BigDecimal("0.00"), response.getTotalInventorySellingValue());
	}

	@Test
	void getInventorySummaryShouldHandleAllProductsAtLowStock() {
		Product first = createProduct(1L, "Keyboard", "KEY-001", new BigDecimal("800.00"), 2);

		Product second = createProduct(2L, "Mouse", "MOU-001", new BigDecimal("500.00"), 4);

		when(productRepository.findAll()).thenReturn(List.of(first, second));

		InventorySummaryResponse response = inventorySummaryService.getInventorySummary();

		assertEquals(2L, response.getTotalProducts());
		assertEquals(2L, response.getLowStockProducts());
		assertEquals(0L, response.getOutOfStockProducts());
		assertEquals(new BigDecimal("3600.00"), response.getTotalInventorySellingValue());
	}

}
