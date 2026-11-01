package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.SaleRequest;
import com.finnexa.dto.SaleResponse;
import com.finnexa.entity.Product;
import com.finnexa.entity.Sale;
import com.finnexa.entity.SaleItem;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.ProductRepository;
import com.finnexa.repository.SaleRepository;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

	@Mock
	private SaleRepository saleRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private SaleService saleService;

	private Product product;
	private SaleRequest saleRequest;

	@BeforeEach
	void setUp() {
		product = new Product();
		product.setId(1L);
		product.setName("Wireless Mouse");
		product.setSku("FNX-MOU-001");
		product.setPrice(new BigDecimal("799.00"));
		product.setQuantity(50);

		saleRequest = new SaleRequest();
		saleRequest.setPaymentMethod(Sale.PaymentMethod.UPI);

		SaleRequest.SaleItemRequest itemRequest = new SaleRequest.SaleItemRequest();

		itemRequest.setProductId(1L);
		itemRequest.setQuantity(2);

		saleRequest.setItems(List.of(itemRequest));
	}

	// 1. Create a sale successfully
	@Test
	void createSaleShouldReturnCreatedSale() {
		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		mockSaleSave();

		SaleResponse response = saleService.createSale(saleRequest);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(new BigDecimal("1598.00"), response.getTotalAmount());
		assertEquals(Sale.PaymentMethod.UPI, response.getPaymentMethod());
		assertEquals(Sale.SaleStatus.COMPLETED, response.getStatus());
		assertNotNull(response.getInvoiceNumber());
		assertEquals(48, product.getQuantity());

		verify(saleRepository).saveAndFlush(any(Sale.class));
		verify(productRepository).save(product);
		verify(auditLogService).recordAction(eq("SALE_CREATED"), eq("Sale"), eq(1L), any(String.class));
	}

	// 2. Generate an invoice number
	@Test
	void createSaleShouldGenerateInvoiceNumber() {
		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		mockSaleSave();

		SaleResponse response = saleService.createSale(saleRequest);

		assertNotNull(response.getInvoiceNumber());
		org.junit.jupiter.api.Assertions.assertTrue(response.getInvoiceNumber().matches("FNX-\\d{14}-[A-F0-9]{8}"));
	}

	// 3. Calculate the total for multiple products
	@Test
	void createSaleShouldCalculateTotalForMultipleProducts() {
		Product secondProduct = new Product();
		secondProduct.setId(2L);
		secondProduct.setName("Keyboard");
		secondProduct.setSku("FNX-KEY-001");
		secondProduct.setPrice(new BigDecimal("1200.00"));
		secondProduct.setQuantity(20);

		SaleRequest.SaleItemRequest secondItem = new SaleRequest.SaleItemRequest();

		secondItem.setProductId(2L);
		secondItem.setQuantity(1);

		saleRequest.setItems(List.of(saleRequest.getItems().get(0), secondItem));

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(secondProduct));

		mockSaleSave();

		SaleResponse response = saleService.createSale(saleRequest);

		assertEquals(new BigDecimal("2798.00"), response.getTotalAmount());
		assertEquals(48, product.getQuantity());
		assertEquals(19, secondProduct.getQuantity());
	}

	// 4. Reject a null sale request
	@Test
	void createSaleShouldRejectNullRequest() {
		assertThrows(BadRequestException.class, () -> saleService.createSale(null));

		verify(saleRepository, never()).saveAndFlush(any(Sale.class));
	}

	// 5. Reject an empty item list
	@Test
	void createSaleShouldRejectEmptyItems() {
		saleRequest.setItems(List.of());

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));

		verify(saleRepository, never()).saveAndFlush(any(Sale.class));
	}

	// 6. Reject a null item list
	@Test
	void createSaleShouldRejectNullItems() {
		saleRequest.setItems(null);

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 7. Reject a missing payment method
	@Test
	void createSaleShouldRejectMissingPaymentMethod() {
		saleRequest.setPaymentMethod(null);

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 8. Reject a null item
	@Test
	void createSaleShouldRejectNullItem() {
		saleRequest.setItems(java.util.Arrays.asList((SaleRequest.SaleItemRequest) null));

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 9. Reject a null product ID
	@Test
	void createSaleShouldRejectNullProductId() {
		saleRequest.getItems().get(0).setProductId(null);

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 10. Reject an invalid product ID
	@Test
	void createSaleShouldRejectInvalidProductId() {
		saleRequest.getItems().get(0).setProductId(0L);

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 11. Reject a null quantity
	@Test
	void createSaleShouldRejectNullQuantity() {
		saleRequest.getItems().get(0).setQuantity(null);

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 12. Reject zero quantity
	@Test
	void createSaleShouldRejectZeroQuantity() {
		saleRequest.getItems().get(0).setQuantity(0);

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 13. Reject negative quantity
	@Test
	void createSaleShouldRejectNegativeQuantity() {
		saleRequest.getItems().get(0).setQuantity(-2);

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 14. Reject duplicate products
	@Test
	void createSaleShouldRejectDuplicateProducts() {
		SaleRequest.SaleItemRequest duplicateItem = new SaleRequest.SaleItemRequest();

		duplicateItem.setProductId(1L);
		duplicateItem.setQuantity(1);

		saleRequest.setItems(List.of(saleRequest.getItems().get(0), duplicateItem));

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));

		verify(saleRepository, never()).saveAndFlush(any(Sale.class));
	}

	// 15. Reject a nonexistent product
	@Test
	void createSaleShouldThrowWhenProductDoesNotExist() {
		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.createSale(saleRequest));

		verify(saleRepository, never()).saveAndFlush(any(Sale.class));
	}

	// 16. Reject a null product price
	@Test
	void createSaleShouldRejectNullProductPrice() {
		product.setPrice(null);

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 17. Reject a zero product price
	@Test
	void createSaleShouldRejectZeroProductPrice() {
		product.setPrice(BigDecimal.ZERO);

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 18. Reject a negative product price
	@Test
	void createSaleShouldRejectNegativeProductPrice() {
		product.setPrice(new BigDecimal("-10.00"));

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 19. Reject insufficient stock
	@Test
	void createSaleShouldRejectInsufficientStock() {
		product.setQuantity(1);

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));

		verify(saleRepository, never()).saveAndFlush(any(Sale.class));
	}

	// 20. Reject null stock quantity
	@Test
	void createSaleShouldRejectNullStockQuantity() {
		product.setQuantity(null);

		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> saleService.createSale(saleRequest));
	}

	// 21. Retrieve all sales
	@Test
	void getAllSalesShouldReturnAllSales() {
		Sale sale = createSaleEntity(1L);

		when(saleRepository.findAll()).thenReturn(List.of(sale));

		List<SaleResponse> responses = saleService.getAllSales();

		assertEquals(1, responses.size());
		assertEquals(sale.getInvoiceNumber(), responses.get(0).getInvoiceNumber());
	}

	// 22. Retrieve a sale by ID
	@Test
	void getSaleByIdShouldReturnSale() {
		Sale sale = createSaleEntity(1L);

		when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));

		SaleResponse response = saleService.getSaleById(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals(sale.getInvoiceNumber(), response.getInvoiceNumber());
	}

	// 23. Handle a missing sale ID
	@Test
	void getSaleByIdShouldThrowWhenSaleDoesNotExist() {
		when(saleRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.getSaleById(999L));
	}

	// 24. Reject invalid sale IDs
	@Test
	void getSaleByIdShouldRejectInvalidId() {
		assertThrows(BadRequestException.class, () -> saleService.getSaleById(0L));

		assertThrows(BadRequestException.class, () -> saleService.getSaleById(-1L));

		assertThrows(BadRequestException.class, () -> saleService.getSaleById(null));
	}

	// 25. Retrieve a sale by invoice number
	@Test
	void getSaleByInvoiceNumberShouldReturnSale() {
		Sale sale = createSaleEntity(1L);

		when(saleRepository.findByInvoiceNumber("FNX-20261026103335-6F1AC4D6")).thenReturn(Optional.of(sale));

		SaleResponse response = saleService.getSaleByInvoiceNumber(" FNX-20261026103335-6F1AC4D6 ");

		assertEquals("FNX-20261026103335-6F1AC4D6", response.getInvoiceNumber());
	}

	// 26. Reject an empty invoice number
	@Test
	void getSaleByInvoiceNumberShouldRejectEmptyInvoiceNumber() {
		assertThrows(BadRequestException.class, () -> saleService.getSaleByInvoiceNumber(" "));
	}

	// 27. Handle an unknown invoice number
	@Test
	void getSaleByInvoiceNumberShouldThrowWhenSaleDoesNotExist() {
		when(saleRepository.findByInvoiceNumber("UNKNOWN-INVOICE")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.getSaleByInvoiceNumber("UNKNOWN-INVOICE"));
	}

	// 28. Retrieve sales between two dates
	@Test
	void getSalesBetweenShouldReturnSalesInDateRange() {
		LocalDateTime start = LocalDateTime.of(2026, 10, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2026, 10, 31, 23, 59);

		Sale sale = createSaleEntity(1L);

		when(saleRepository.findBySaleDateBetween(start, end)).thenReturn(List.of(sale));

		List<SaleResponse> responses = saleService.getSalesBetween(start, end);

		assertEquals(1, responses.size());
		verify(saleRepository).findBySaleDateBetween(start, end);
	}

	// 29. Reject a null start date
	@Test
	void getSalesBetweenShouldRejectNullStartDate() {
		LocalDateTime end = LocalDateTime.now();

		assertThrows(BadRequestException.class, () -> saleService.getSalesBetween(null, end));
	}

	// 30. Reject a null end date
	@Test
	void getSalesBetweenShouldRejectNullEndDate() {
		LocalDateTime start = LocalDateTime.now();

		assertThrows(BadRequestException.class, () -> saleService.getSalesBetween(start, null));
	}

	// 31. Reject a reversed date range
	@Test
	void getSalesBetweenShouldRejectReversedDateRange() {
		LocalDateTime start = LocalDateTime.of(2026, 10, 31, 0, 0);
		LocalDateTime end = LocalDateTime.of(2026, 10, 1, 0, 0);

		assertThrows(BadRequestException.class, () -> saleService.getSalesBetween(start, end));
	}

	private void mockSaleSave() {
		when(saleRepository.saveAndFlush(any(Sale.class))).thenAnswer(invocation -> {
			Sale savedSale = invocation.getArgument(0);
			savedSale.setId(1L);
			return savedSale;
		});
	}

	// 32. Successfully cancel a sale and restore stock
	@Test
	void cancelSaleShouldRestoreStockAndMarkSaleCancelled() {
		Sale sale = createSaleEntity(1L);

		SaleItem item = new SaleItem();
		item.setProduct(product);
		item.setQuantity(2);
		sale.addItem(item);

		product.setQuantity(48);

		when(saleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sale));
		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
		when(saleRepository.save(sale)).thenReturn(sale);

		SaleResponse response = saleService.cancelSale(1L);

		assertNotNull(response);
		assertEquals(Sale.SaleStatus.CANCELLED, sale.getStatus());
		assertEquals(50, product.getQuantity());

		verify(productRepository).save(product);
		verify(saleRepository).save(sale);
		verify(auditLogService).recordAction(eq("SALE_CANCELLED"), eq("Sale"), eq(1L), any(String.class));
	}

	// 33. Reject cancellation of an already-cancelled sale
	@Test
	void cancelSaleShouldRejectAlreadyCancelledSale() {
		Sale sale = createSaleEntity(1L);
		sale.setStatus(Sale.SaleStatus.CANCELLED);

		when(saleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sale));

		assertThrows(BadRequestException.class, () -> saleService.cancelSale(1L));

		verify(productRepository, never()).save(any(Product.class));
		verify(saleRepository, never()).save(any(Sale.class));
	}

	// 34. Reject cancellation when sale does not exist
	@Test
	void cancelSaleShouldThrowWhenSaleDoesNotExist() {
		when(saleRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> saleService.cancelSale(999L));
	}

	// 35. Reject an invalid sale ID
	@Test
	void cancelSaleShouldRejectInvalidSaleId() {
		assertThrows(BadRequestException.class, () -> saleService.cancelSale(0L));

		assertThrows(BadRequestException.class, () -> saleService.cancelSale(-1L));

		assertThrows(BadRequestException.class, () -> saleService.cancelSale(null));

		verify(saleRepository, never()).findByIdForUpdate(any(Long.class));
	}

	// 36. Reject cancellation if restoring stock would overflow
	@Test
	void cancelSaleShouldRejectStockOverflow() {
		Sale sale = createSaleEntity(1L);

		SaleItem item = new SaleItem();
		item.setProduct(product);
		item.setQuantity(2);
		sale.addItem(item);

		product.setQuantity(Integer.MAX_VALUE);

		when(saleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sale));
		when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

		assertThrows(BadRequestException.class, () -> saleService.cancelSale(1L));

		assertEquals(Integer.MAX_VALUE, product.getQuantity());
		assertEquals(Sale.SaleStatus.COMPLETED, sale.getStatus());

		verify(productRepository, never()).save(any(Product.class));
		verify(saleRepository, never()).save(any(Sale.class));
	}

	private Sale createSaleEntity(Long id) {
		Sale sale = new Sale();
		sale.setId(id);
		sale.setInvoiceNumber("FNX-20261026103335-6F1AC4D6");
		sale.setTotalAmount(new BigDecimal("1598.00"));
		sale.setPaymentMethod(Sale.PaymentMethod.UPI);
		sale.setStatus(Sale.SaleStatus.COMPLETED);
		return sale;
	}
}