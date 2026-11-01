package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;

import static org.mockito.Mockito.*;

import java.util.List;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.InventoryAdjustmentRequest;

import com.finnexa.dto.InventoryAdjustmentResponse;

import com.finnexa.entity.InventoryAdjustment;

import com.finnexa.entity.Product;

import com.finnexa.exception.BadRequestException;

import com.finnexa.exception.ResourceNotFoundException;

import com.finnexa.repository.InventoryAdjustmentRepository;

import com.finnexa.repository.ProductRepository;
import com.finnexa.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)

class InventoryAdjustmentServiceTest {

	@Mock

	private InventoryAdjustmentRepository adjustmentRepository;

	@Mock

	private ProductRepository productRepository;

	@Mock

	private AuditLogService auditLogService;

	@InjectMocks

	private InventoryAdjustmentService inventoryAdjustmentService;

	private Product product;

	private InventoryAdjustmentRequest request;

	@BeforeEach

	void setUp() {

		product = createProduct(1L, "Keyboard", "KEY-001", 10);

		request = new InventoryAdjustmentRequest();

		request.setProductId(1L);

		request.setQuantityChange(5);

		request.setReason("Stock correction");

	}

	private Product createProduct(Long id, String name, String sku, int quantity) {

		Product product = new Product();

		product.setId(id);

		product.setName(name);

		product.setSku(sku);

		product.setQuantity(quantity);

		return product;

	}

	private InventoryAdjustment createAdjustment(Product product, int quantityChange, String reason) {

		InventoryAdjustment adjustment = new InventoryAdjustment();

		adjustment.setProduct(product);

		adjustment.setQuantityChange(quantityChange);

		adjustment.setReason(reason);

		// ID and adjustedAt are generated during actual JPA persistence.

		return adjustment;

	}

	private void mockSuccessfulAdjustment() {

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		when(adjustmentRepository.save(any(InventoryAdjustment.class)))

				.thenAnswer(invocation -> invocation.getArgument(0));

	}

// =========================================================

// 1. STOCK ADJUSTMENT SUCCESS TESTS

// =========================================================

	@Test

	void adjustStockShouldIncreaseStockSuccessfully() {

		mockSuccessfulAdjustment();

		InventoryAdjustmentResponse response = inventoryAdjustmentService.adjustStock(request);

		assertNotNull(response);

		assertEquals(1L, response.getProductId());

		assertEquals("Keyboard", response.getProductName());

		assertEquals(5, response.getQuantityChange());

		assertEquals(10, response.getPreviousQuantity());

		assertEquals(15, response.getNewQuantity());

		assertEquals("Stock correction", response.getReason());

		assertEquals(15, product.getQuantity());

	}

	@Test

	void adjustStockShouldDecreaseStockSuccessfully() {

		request.setQuantityChange(-3);

		request.setReason("Damaged items");

		mockSuccessfulAdjustment();

		InventoryAdjustmentResponse response = inventoryAdjustmentService.adjustStock(request);

		assertEquals(-3, response.getQuantityChange());

		assertEquals(10, response.getPreviousQuantity());

		assertEquals(7, response.getNewQuantity());

		assertEquals(7, product.getQuantity());

	}

	@Test

	void adjustStockShouldAllowStockToBecomeZero() {

		product.setQuantity(5);

		request.setQuantityChange(-5);

		mockSuccessfulAdjustment();

		InventoryAdjustmentResponse response = inventoryAdjustmentService.adjustStock(request);

		assertEquals(0, response.getNewQuantity());

		assertEquals(0, product.getQuantity());

	}

	@Test

	void adjustStockShouldTrimReason() {

		request.setReason("  Stock correction  ");

		mockSuccessfulAdjustment();

		InventoryAdjustmentResponse response = inventoryAdjustmentService.adjustStock(request);

		assertEquals("Stock correction", response.getReason());

	}

	@Test

	void adjustStockShouldSaveUpdatedProduct() {

		mockSuccessfulAdjustment();

		inventoryAdjustmentService.adjustStock(request);

		verify(productRepository).save(product);

		assertEquals(15, product.getQuantity());

	}

	@Test

	void adjustStockShouldSaveAdjustmentRecord() {

		mockSuccessfulAdjustment();

		inventoryAdjustmentService.adjustStock(request);

		verify(adjustmentRepository).save(any(InventoryAdjustment.class));

	}

	@Test

	void adjustStockShouldSaveCorrectAdjustmentDetails() {

		mockSuccessfulAdjustment();

		inventoryAdjustmentService.adjustStock(request);

		verify(adjustmentRepository).save(argThat(adjustment -> adjustment.getProduct() == product

				&& adjustment.getQuantityChange() == 5 && "Stock correction".equals(adjustment.getReason())));

	}

	@Test

	void adjustStockShouldRecordAuditLog() {

		mockSuccessfulAdjustment();

		inventoryAdjustmentService.adjustStock(request);

		verify(auditLogService).recordAction(eq("STOCK_ADJUSTMENT"), eq("Product"), eq(1L),

				contains("Previous quantity: 10"));

	}

	@Test

	void adjustStockAuditLogShouldContainNewQuantity() {

		mockSuccessfulAdjustment();

		inventoryAdjustmentService.adjustStock(request);

		verify(auditLogService).recordAction(eq("STOCK_ADJUSTMENT"), eq("Product"), eq(1L),

				contains("New quantity: 15"));

	}

	@Test

	void adjustStockAuditLogShouldContainQuantityChange() {

		mockSuccessfulAdjustment();

		inventoryAdjustmentService.adjustStock(request);

		verify(auditLogService).recordAction(eq("STOCK_ADJUSTMENT"), eq("Product"), eq(1L),

				contains("Quantity change: 5"));

	}

	@Test

	void adjustStockAuditLogShouldContainReason() {

		mockSuccessfulAdjustment();

		inventoryAdjustmentService.adjustStock(request);

		verify(auditLogService).recordAction(eq("STOCK_ADJUSTMENT"), eq("Product"), eq(1L),

				contains("Reason: Stock correction"));

	}

// =========================================================

// 2. REQUEST VALIDATION TESTS

// =========================================================

	@Test

	void adjustStockShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(null));

	}

	@Test

	void adjustStockShouldRejectNullProductId() {

		request.setProductId(null);

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

	}

	@Test

	void adjustStockShouldRejectNullQuantityChange() {

		request.setQuantityChange(null);

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

	}

	@Test

	void adjustStockShouldRejectNullReason() {

		request.setReason(null);

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

	}

	@Test

	void adjustStockShouldRejectBlankReason() {

		request.setReason("   ");

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

	}

	@Test

	void adjustStockShouldRejectZeroQuantityChange() {

		request.setQuantityChange(0);

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

	}

	@Test

	void adjustStockShouldRejectMissingProduct() {

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> inventoryAdjustmentService.adjustStock(request));

	}

	@Test

	void adjustStockShouldNotSaveProductForInvalidRequest() {

		request.setQuantityChange(0);

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

		verify(productRepository, never()).save(any(Product.class));

		verify(adjustmentRepository, never()).save(any(InventoryAdjustment.class));

	}

	@Test

	void adjustStockShouldRejectNegativeResultingStock() {

		product.setQuantity(5);

		request.setQuantityChange(-6);

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

		assertEquals(5, product.getQuantity());

		verify(productRepository, never()).save(any(Product.class));

		verify(adjustmentRepository, never()).save(any(InventoryAdjustment.class));

	}

	@Test

	void adjustStockShouldRejectIntegerOverflow() {

		product.setQuantity(Integer.MAX_VALUE);

		request.setQuantityChange(1);

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.adjustStock(request));

		assertEquals(Integer.MAX_VALUE, product.getQuantity());

		verify(productRepository, never()).save(any(Product.class));

		verify(adjustmentRepository, never()).save(any(InventoryAdjustment.class));

	}

	@Test

	void adjustStockShouldAllowIntegerMaximumQuantity() {

		product.setQuantity(Integer.MAX_VALUE - 1);

		request.setQuantityChange(1);

		mockSuccessfulAdjustment();

		InventoryAdjustmentResponse response = inventoryAdjustmentService.adjustStock(request);

		assertEquals(Integer.MAX_VALUE, response.getNewQuantity());

		assertEquals(Integer.MAX_VALUE, product.getQuantity());

	}

	@Test

	void adjustStockShouldRejectMissingProductWithoutSavingAdjustment() {

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> inventoryAdjustmentService.adjustStock(request));

		verify(adjustmentRepository, never()).save(any(InventoryAdjustment.class));

	}

// =========================================================

// 3. GET ALL ADJUSTMENTS TESTS

// =========================================================

	@Test

	void getAllAdjustmentsShouldReturnAllAdjustments() {

		InventoryAdjustment first = createAdjustment(product, 5, "Stock correction");

		Product secondProduct = createProduct(2L, "Mouse", "MOU-001", 20);

		InventoryAdjustment second = createAdjustment(secondProduct, 10, "New stock");

		when(adjustmentRepository.findAllByOrderByAdjustedAtDesc()).thenReturn(List.of(first, second));

		List<InventoryAdjustmentResponse> responses = inventoryAdjustmentService.getAllAdjustments();

		assertEquals(2, responses.size());

		assertEquals(1L, responses.get(0).getProductId());

		assertEquals(2L, responses.get(1).getProductId());

	}

	@Test

	void getAllAdjustmentsShouldReturnEmptyList() {

		when(adjustmentRepository.findAllByOrderByAdjustedAtDesc()).thenReturn(List.of());

		List<InventoryAdjustmentResponse> responses = inventoryAdjustmentService.getAllAdjustments();

		assertNotNull(responses);

		assertTrue(responses.isEmpty());

	}

	@Test

	void getAllAdjustmentsShouldMapProductInformation() {

		InventoryAdjustment adjustment = createAdjustment(product, 5, "Stock correction");

		when(adjustmentRepository.findAllByOrderByAdjustedAtDesc()).thenReturn(List.of(adjustment));

		InventoryAdjustmentResponse response = inventoryAdjustmentService.getAllAdjustments().get(0);

		assertEquals(1L, response.getProductId());

		assertEquals("Keyboard", response.getProductName());

		assertEquals(5, response.getQuantityChange());

		assertEquals("Stock correction", response.getReason());

	}

	@Test

	void getAllAdjustmentsShouldCalculateMappedQuantities() {

		product.setQuantity(15);

		InventoryAdjustment adjustment = createAdjustment(product, 5, "Stock correction");

		when(adjustmentRepository.findAllByOrderByAdjustedAtDesc()).thenReturn(List.of(adjustment));

		InventoryAdjustmentResponse response = inventoryAdjustmentService.getAllAdjustments().get(0);

		assertEquals(10, response.getPreviousQuantity());

		assertEquals(15, response.getNewQuantity());

	}

	@Test

	void getAllAdjustmentsShouldHandleNegativeQuantityChange() {

		product.setQuantity(7);

		InventoryAdjustment adjustment = createAdjustment(product, -3, "Damaged items");

		when(adjustmentRepository.findAllByOrderByAdjustedAtDesc()).thenReturn(List.of(adjustment));

		InventoryAdjustmentResponse response = inventoryAdjustmentService.getAllAdjustments().get(0);

		assertEquals(-3, response.getQuantityChange());

		assertEquals(10, response.getPreviousQuantity());

		assertEquals(7, response.getNewQuantity());

	}

	@Test

	void getAllAdjustmentsShouldUseCurrentProductName() {

		product.setName("Updated Keyboard");

		InventoryAdjustment adjustment = createAdjustment(product, 5, "Stock correction");

		when(adjustmentRepository.findAllByOrderByAdjustedAtDesc()).thenReturn(List.of(adjustment));

		InventoryAdjustmentResponse response = inventoryAdjustmentService.getAllAdjustments().get(0);

		assertEquals("Updated Keyboard", response.getProductName());

	}

// =========================================================

// 4. GET ADJUSTMENTS BY PRODUCT ID TESTS

// =========================================================

	@Test

	void getAdjustmentsByProductIdShouldReturnAdjustments() {

		InventoryAdjustment adjustment = createAdjustment(product, 5, "Stock correction");

		when(productRepository.existsById(1L)).thenReturn(true);

		when(adjustmentRepository.findByProductIdOrderByAdjustedAtDesc(1L)).thenReturn(List.of(adjustment));

		List<InventoryAdjustmentResponse> responses = inventoryAdjustmentService.getAdjustmentsByProductId(1L);

		assertEquals(1, responses.size());

		assertEquals(1L, responses.get(0).getProductId());

		assertEquals("Keyboard", responses.get(0).getProductName());

	}

	@Test

	void getAdjustmentsByProductIdShouldReturnEmptyList() {

		when(productRepository.existsById(1L)).thenReturn(true);

		when(adjustmentRepository.findByProductIdOrderByAdjustedAtDesc(1L)).thenReturn(List.of());

		List<InventoryAdjustmentResponse> responses = inventoryAdjustmentService.getAdjustmentsByProductId(1L);

		assertNotNull(responses);

		assertTrue(responses.isEmpty());

	}

	@Test
	void getAdjustmentsByProductIdShouldRejectNullId() {
		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.getAdjustmentsByProductId(null));

		verifyNoInteractions(productRepository, adjustmentRepository);
	}

	@Test

	void getAdjustmentsByProductIdShouldRejectMissingProduct() {

		when(productRepository.existsById(999L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> inventoryAdjustmentService.getAdjustmentsByProductId(999L));

	}

	@Test
	void getAdjustmentsByProductIdShouldRejectZeroIdWhenProductMissing() {
		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.getAdjustmentsByProductId(0L));

		verifyNoInteractions(productRepository, adjustmentRepository);
	}

	@Test
	void getAdjustmentsByProductIdShouldRejectNegativeIdWhenProductMissing() {
		assertThrows(BadRequestException.class, () -> inventoryAdjustmentService.getAdjustmentsByProductId(-1L));

		verifyNoInteractions(productRepository, adjustmentRepository);
	}

	@Test

	void getAdjustmentsByProductIdShouldCheckProductExistence() {

		when(productRepository.existsById(1L)).thenReturn(true);

		when(adjustmentRepository.findByProductIdOrderByAdjustedAtDesc(1L)).thenReturn(List.of());

		inventoryAdjustmentService.getAdjustmentsByProductId(1L);

		verify(productRepository).existsById(1L);

	}

	@Test

	void getAdjustmentsByProductIdShouldQueryCorrectProduct() {

		when(productRepository.existsById(1L)).thenReturn(true);

		when(adjustmentRepository.findByProductIdOrderByAdjustedAtDesc(1L)).thenReturn(List.of());

		inventoryAdjustmentService.getAdjustmentsByProductId(1L);

		verify(adjustmentRepository).findByProductIdOrderByAdjustedAtDesc(1L);

	}

}