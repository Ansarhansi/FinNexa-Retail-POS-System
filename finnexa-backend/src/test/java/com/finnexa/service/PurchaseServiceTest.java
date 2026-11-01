package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;

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

import com.finnexa.dto.PurchaseRequest;

import com.finnexa.dto.PurchaseResponse;

import com.finnexa.entity.Product;

import com.finnexa.entity.Purchase;

import com.finnexa.entity.PurchaseItem;

import com.finnexa.entity.Supplier;

import com.finnexa.exception.BadRequestException;

import com.finnexa.exception.ResourceNotFoundException;

import com.finnexa.repository.ProductRepository;

import com.finnexa.repository.PurchaseRepository;

import com.finnexa.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)

class PurchaseServiceTest {

	@Mock

	private PurchaseRepository purchaseRepository;

	@Mock

	private ProductRepository productRepository;

	@Mock

	private SupplierRepository supplierRepository;

	@Mock

	private AuditLogService auditLogService;

	@InjectMocks

	private PurchaseService purchaseService;

	private Supplier supplier;

	private Product product;

	private PurchaseRequest purchaseRequest;

	@BeforeEach

	void setUp() {

		supplier = new Supplier();

		supplier.setId(1L);

		supplier.setName("TechWorld Distributors");

		product = new Product();

		product.setId(1L);

		product.setName("Keyboard");

		product.setSku("KEY-001");

		product.setPrice(new BigDecimal("800.00"));

		product.setQuantity(10);

		PurchaseRequest.PurchaseItemRequest itemRequest = new PurchaseRequest.PurchaseItemRequest();

		itemRequest.setProductId(1L);

		itemRequest.setQuantity(5);

		itemRequest.setUnitCost(new BigDecimal("250.00"));

		purchaseRequest = new PurchaseRequest();

		purchaseRequest.setSupplierId(1L);

		purchaseRequest.setItems(List.of(itemRequest));

	}

	private Purchase createPurchase(Long id, Purchase.PurchaseStatus status) {

		Purchase purchase = new Purchase();

		purchase.setId(id);

		purchase.setPurchaseNumber("PUR-20261026103931-8987C459");

		purchase.setSupplier(supplier);

		purchase.setStatus(status);

		purchase.setTotalAmount(new BigDecimal("1250.00"));

		PurchaseItem item = new PurchaseItem();

		item.setProduct(product);

		item.setQuantity(5);

		item.setUnitCost(new BigDecimal("250.00"));

		item.calculateSubtotal();

		purchase.addItem(item);

		return purchase;

	}

	private void mockSuccessfulPurchaseCreation() {

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {

			Purchase purchase = invocation.getArgument(0);

			purchase.setId(1L);

			return purchase;

		});

	}

// ---------------------------------------------------------

// 1. PURCHASE CREATION TESTS

// ---------------------------------------------------------

	@Test

	void createPurchaseShouldCreatePurchaseSuccessfully() {

		mockSuccessfulPurchaseCreation();

		PurchaseResponse response = purchaseService.createPurchase(purchaseRequest);

		assertNotNull(response);

		assertEquals(1L, response.getId());

		assertEquals(new BigDecimal("1250.00"), response.getTotalAmount());

		assertEquals(Purchase.PurchaseStatus.PENDING, response.getStatus());

		verify(purchaseRepository).save(any(Purchase.class));

		verify(auditLogService).recordAction(eq("PURCHASE_CREATED"), eq("Purchase"), eq(1L), anyString());

	}

	@Test

	void createPurchaseShouldGenerateValidPurchaseNumber() {

		mockSuccessfulPurchaseCreation();

		PurchaseResponse response = purchaseService.createPurchase(purchaseRequest);

		assertTrue(response.getPurchaseNumber().matches("PUR-\\d{14}-[A-F0-9]{8}"));

	}

	@Test

	void createPurchaseShouldCalculateTotalForMultipleItems() {

		PurchaseRequest.PurchaseItemRequest secondItem = new PurchaseRequest.PurchaseItemRequest();

		secondItem.setProductId(2L);

		secondItem.setQuantity(3);

		secondItem.setUnitCost(new BigDecimal("100.00"));

		purchaseRequest.setItems(List.of(purchaseRequest.getItems().get(0), secondItem));

		Product secondProduct = new Product();

		secondProduct.setId(2L);

		secondProduct.setName("Mouse");

		secondProduct.setSku("MOU-001");

		secondProduct.setPrice(new BigDecimal("500.00"));

		secondProduct.setQuantity(20);

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		when(productRepository.findById(2L)).thenReturn(Optional.of(secondProduct));

		when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {

			Purchase purchase = invocation.getArgument(0);

			purchase.setId(1L);

			return purchase;

		});

		PurchaseResponse response = purchaseService.createPurchase(purchaseRequest);

		assertEquals(new BigDecimal("1550.00"), response.getTotalAmount());

		assertEquals(2, response.getItems().size());

	}

	@Test

	void createPurchaseShouldNotIncreaseStockImmediately() {

		mockSuccessfulPurchaseCreation();

		int originalQuantity = product.getQuantity();

		purchaseService.createPurchase(purchaseRequest);

		assertEquals(originalQuantity, product.getQuantity());

		verify(productRepository, never()).save(any(Product.class));

	}

	@Test

	void createPurchaseShouldSavePurchaseAsPending() {

		mockSuccessfulPurchaseCreation();

		PurchaseResponse response = purchaseService.createPurchase(purchaseRequest);

		assertEquals(Purchase.PurchaseStatus.PENDING, response.getStatus());

	}

	@Test

	void createPurchaseShouldRecordAuditLog() {

		mockSuccessfulPurchaseCreation();

		purchaseService.createPurchase(purchaseRequest);

		verify(auditLogService).recordAction(eq("PURCHASE_CREATED"), eq("Purchase"), eq(1L),

				contains("TechWorld Distributors"));

	}

	@Test

	void createPurchaseShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(null));

	}

	@Test

	void createPurchaseShouldRejectNullSupplierId() {

		purchaseRequest.setSupplierId(null);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectZeroSupplierId() {

		purchaseRequest.setSupplierId(0L);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNegativeSupplierId() {

		purchaseRequest.setSupplierId(-1L);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNullItems() {

		purchaseRequest.setItems(null);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectEmptyItems() {

		purchaseRequest.setItems(List.of());

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNullItem() {

		purchaseRequest.setItems(java.util.Arrays.asList((PurchaseRequest.PurchaseItemRequest) null));

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNullProductId() {

		purchaseRequest.getItems().get(0).setProductId(null);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectZeroProductId() {

		purchaseRequest.getItems().get(0).setProductId(0L);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNegativeProductId() {

		purchaseRequest.getItems().get(0).setProductId(-1L);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNullQuantity() {

		purchaseRequest.getItems().get(0).setQuantity(null);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectZeroQuantity() {

		purchaseRequest.getItems().get(0).setQuantity(0);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNegativeQuantity() {

		purchaseRequest.getItems().get(0).setQuantity(-5);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNullUnitCost() {

		purchaseRequest.getItems().get(0).setUnitCost(null);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectZeroUnitCost() {

		purchaseRequest.getItems().get(0).setUnitCost(BigDecimal.ZERO);

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectNegativeUnitCost() {

		purchaseRequest.getItems().get(0).setUnitCost(new BigDecimal("-10.00"));

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectDuplicateProducts() {

		PurchaseRequest.PurchaseItemRequest duplicate = new PurchaseRequest.PurchaseItemRequest();

		duplicate.setProductId(1L);

		duplicate.setQuantity(2);

		duplicate.setUnitCost(new BigDecimal("100.00"));

		purchaseRequest.setItems(List.of(purchaseRequest.getItems().get(0), duplicate));

		assertThrows(BadRequestException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectMissingSupplier() {

		when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

	@Test

	void createPurchaseShouldRejectMissingProduct() {

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		when(productRepository.findById(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.createPurchase(purchaseRequest));

	}

// ---------------------------------------------------------

// 2. RETRIEVAL TESTS

// ---------------------------------------------------------

	@Test

	void getAllPurchasesShouldReturnAllPurchases() {

		Purchase first = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		Purchase second = createPurchase(2L, Purchase.PurchaseStatus.RECEIVED);

		when(purchaseRepository.findAll()).thenReturn(List.of(first, second));

		List<PurchaseResponse> responses = purchaseService.getAllPurchases();

		assertEquals(2, responses.size());

	}

	@Test

	void getAllPurchasesShouldReturnEmptyList() {

		when(purchaseRepository.findAll()).thenReturn(List.of());

		List<PurchaseResponse> responses = purchaseService.getAllPurchases();

		assertNotNull(responses);

		assertTrue(responses.isEmpty());

	}

	@Test

	void getPurchaseByIdShouldReturnPurchase() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

		PurchaseResponse response = purchaseService.getPurchaseById(1L);

		assertEquals(1L, response.getId());

		assertEquals(Purchase.PurchaseStatus.PENDING, response.getStatus());

	}

	@Test

	void getPurchaseByIdShouldRejectNullId() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchaseById(null));

	}

	@Test

	void getPurchaseByIdShouldRejectZeroId() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchaseById(0L));

	}

	@Test

	void getPurchaseByIdShouldRejectNegativeId() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchaseById(-1L));

	}

	@Test

	void getPurchaseByIdShouldRejectMissingPurchase() {

		when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.getPurchaseById(999L));

	}

	@Test

	void getPurchaseByNumberShouldReturnPurchase() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findByPurchaseNumber("PUR-20261026103931-8987C459")).thenReturn(Optional.of(purchase));

		PurchaseResponse response = purchaseService.getPurchaseByNumber("PUR-20261026103931-8987C459");

		assertEquals("PUR-20261026103931-8987C459", response.getPurchaseNumber());

	}

	@Test

	void getPurchaseByNumberShouldTrimNumber() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findByPurchaseNumber("PUR-20261026103931-8987C459")).thenReturn(Optional.of(purchase));

		PurchaseResponse response = purchaseService.getPurchaseByNumber(" PUR-20261026103931-8987C459 ");

		assertNotNull(response);

	}

	@Test

	void getPurchaseByNumberShouldRejectNullNumber() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchaseByNumber(null));

	}

	@Test

	void getPurchaseByNumberShouldRejectBlankNumber() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchaseByNumber("   "));

	}

	@Test

	void getPurchaseByNumberShouldRejectMissingPurchase() {

		when(purchaseRepository.findByPurchaseNumber("UNKNOWN")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.getPurchaseByNumber("UNKNOWN"));

	}

	@Test

	void getPurchasesBySupplierShouldReturnPurchases() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(supplierRepository.existsById(1L)).thenReturn(true);

		when(purchaseRepository.findBySupplierId(1L)).thenReturn(List.of(purchase));

		List<PurchaseResponse> responses = purchaseService.getPurchasesBySupplier(1L);

		assertEquals(1, responses.size());

	}

	@Test

	void getPurchasesBySupplierShouldReturnEmptyList() {

		when(supplierRepository.existsById(1L)).thenReturn(true);

		when(purchaseRepository.findBySupplierId(1L)).thenReturn(List.of());

		List<PurchaseResponse> responses = purchaseService.getPurchasesBySupplier(1L);

		assertTrue(responses.isEmpty());

	}

	@Test

	void getPurchasesBySupplierShouldRejectNullId() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchasesBySupplier(null));

	}

	@Test

	void getPurchasesBySupplierShouldRejectZeroId() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchasesBySupplier(0L));

	}

	@Test

	void getPurchasesBySupplierShouldRejectNegativeId() {

		assertThrows(BadRequestException.class, () -> purchaseService.getPurchasesBySupplier(-1L));

	}

	@Test

	void getPurchasesBySupplierShouldRejectMissingSupplier() {

		when(supplierRepository.existsById(999L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.getPurchasesBySupplier(999L));

	}

// ---------------------------------------------------------

// 3. RECEIVE PURCHASE TESTS

// ---------------------------------------------------------

	@Test

	void receivePurchaseShouldIncreaseStockAndUpdateStatus() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PurchaseResponse response = purchaseService.receivePurchase(1L);

		assertEquals(15, product.getQuantity());

		assertEquals(Purchase.PurchaseStatus.RECEIVED, response.getStatus());

		verify(productRepository).save(product);

		verify(auditLogService).recordAction(eq("PURCHASE_RECEIVED"), eq("Purchase"), eq(1L),

				contains("Inventory stock increased."));

	}

	@Test

	void receivePurchaseShouldRejectAlreadyReceivedPurchase() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.RECEIVED);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		assertThrows(BadRequestException.class, () -> purchaseService.receivePurchase(1L));

		verify(productRepository, never()).save(any(Product.class));

	}

	@Test

	void receivePurchaseShouldRejectCancelledPurchase() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.CANCELLED);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		assertThrows(BadRequestException.class, () -> purchaseService.receivePurchase(1L));

	}

	@Test

	void receivePurchaseShouldRejectInvalidStockQuantity() {

		product.setQuantity(-1);

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> purchaseService.receivePurchase(1L));

	}

	@Test

	void receivePurchaseShouldRejectNullStockQuantity() {

		product.setQuantity(null);

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> purchaseService.receivePurchase(1L));

	}

	@Test

	void receivePurchaseShouldRejectMissingProduct() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.receivePurchase(1L));

	}

	@Test

	void receivePurchaseShouldRejectNullPurchaseId() {

		assertThrows(BadRequestException.class, () -> purchaseService.receivePurchase(null));

	}

	@Test

	void receivePurchaseShouldRejectNonexistentPurchase() {

		when(purchaseRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.receivePurchase(999L));

	}

// ---------------------------------------------------------

// 4. CANCEL PURCHASE TESTS

// ---------------------------------------------------------

	@Test

	void cancelPurchaseShouldCancelPendingPurchase() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.PENDING);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PurchaseResponse response = purchaseService.cancelPurchase(1L);

		assertEquals(Purchase.PurchaseStatus.CANCELLED, response.getStatus());

		verify(auditLogService).recordAction(eq("PURCHASE_CANCELLED"), eq("Purchase"), eq(1L), contains("CANCELLED"));

	}

	@Test

	void cancelPurchaseShouldRejectAlreadyCancelledPurchase() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.CANCELLED);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		assertThrows(BadRequestException.class, () -> purchaseService.cancelPurchase(1L));

	}

	@Test

	void cancelPurchaseShouldRejectReceivedPurchase() {

		Purchase purchase = createPurchase(1L, Purchase.PurchaseStatus.RECEIVED);

		when(purchaseRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(purchase));

		assertThrows(BadRequestException.class, () -> purchaseService.cancelPurchase(1L));

	}

	@Test

	void cancelPurchaseShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> purchaseService.cancelPurchase(0L));

	}

	@Test

	void cancelPurchaseShouldRejectMissingPurchase() {

		when(purchaseRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> purchaseService.cancelPurchase(999L));

	}

}