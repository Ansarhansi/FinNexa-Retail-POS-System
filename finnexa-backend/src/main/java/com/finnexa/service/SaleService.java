package com.finnexa.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.SaleRequest;
import com.finnexa.dto.SaleResponse;
import com.finnexa.entity.Product;
import com.finnexa.entity.Sale;
import com.finnexa.entity.SaleItem;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.ProductRepository;
import com.finnexa.repository.SaleRepository;

@Service
@Transactional
public class SaleService {

	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final AuditLogService auditLogService;

	/**
	 * Cancel a completed sale and restore its inventory. All changes execute in one
	 * database transaction.
	 */
	public SaleResponse cancelSale(Long saleId) {

		if (saleId == null || saleId <= 0) {
			throw new BadRequestException("Sale ID must be a positive number.");
		}

		// Lock the sale to prevent simultaneous cancellation.
		Sale sale = saleRepository.findByIdForUpdate(saleId)
				.orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + saleId));

		if (sale.getStatus() != Sale.SaleStatus.COMPLETED) {
			throw new BadRequestException("Only completed sales can be cancelled.");
		}

		// Aggregate quantities in case legacy data contains duplicate products.
		Map<Long, Integer> quantitiesByProduct = sale.getItems().stream()
				.collect(Collectors.toMap(item -> item.getProduct().getId(), SaleItem::getQuantity, Math::addExact));

		// Lock products in ascending ID order to reduce deadlock risk.
		List<Long> productIds = quantitiesByProduct.keySet().stream().sorted(Comparator.naturalOrder()).toList();

		Map<Long, Product> lockedProducts = productIds.stream()
				.map(productId -> productRepository.findByIdForUpdate(productId)
						.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId)))
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		// Validate every resulting quantity before modifying any product.
		for (Long productId : productIds) {
			Product product = lockedProducts.get(productId);
			Integer quantitySold = quantitiesByProduct.get(productId);

			if (quantitySold == null || quantitySold <= 0) {
				throw new BadRequestException("Invalid sale quantity for product: " + product.getName());
			}

			if (product.getQuantity() == null || product.getQuantity() < 0) {
				throw new BadRequestException("Product has an invalid stock quantity: " + product.getName());
			}

			try {
				Math.addExact(product.getQuantity(), quantitySold);
			} catch (ArithmeticException ex) {
				throw new BadRequestException("Stock quantity overflow for product: " + product.getName());
			}
		}

		// Restore stock.
		for (Long productId : productIds) {
			Product product = lockedProducts.get(productId);
			int restoredQuantity = Math.addExact(product.getQuantity(), quantitiesByProduct.get(productId));

			product.setQuantity(restoredQuantity);
			productRepository.save(product);
		}

		// Change status only after all stock validations succeed.
		sale.setStatus(Sale.SaleStatus.CANCELLED);
		Sale cancelledSale = saleRepository.save(sale);

		auditLogService.recordAction("SALE_CANCELLED", "Sale", cancelledSale.getId(),
				"Invoice: " + cancelledSale.getInvoiceNumber() + "; Total amount: " + cancelledSale.getTotalAmount()
						+ "; Inventory stock restored.");

		return new SaleResponse(cancelledSale);
	}

	public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
			AuditLogService auditLogService) {

		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
		this.auditLogService = auditLogService;
	}

// Create a sale, calculate totals, reduce inventory, and record the audit.
	public SaleResponse createSale(SaleRequest request) {

		if (request == null || request.getItems() == null || request.getItems().isEmpty()) {

			throw new BadRequestException("A sale must contain at least one item.");
		}

		if (request.getPaymentMethod() == null) {
			throw new BadRequestException("Payment method is required.");
		}

		// Validate sale items and reject duplicate products.
		Set<Long> productIds = new HashSet<>();

		for (SaleRequest.SaleItemRequest itemRequest : request.getItems()) {

			if (itemRequest == null || itemRequest.getProductId() == null || itemRequest.getProductId() <= 0) {

				throw new BadRequestException("Every sale item must have a valid product ID.");
			}

			if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {

				throw new BadRequestException("Every sale item must have a quantity greater than zero.");
			}

			if (!productIds.add(itemRequest.getProductId())) {
				throw new BadRequestException(
						"A product cannot appear more than once in a sale. " + "Combine its quantities into one item.");
			}
		}

		/*
		 * Acquire product locks in ascending ID order. A consistent lock order helps
		 * reduce deadlock risk when different sales contain overlapping products.
		 */
		List<Long> sortedProductIds = new ArrayList<>(productIds);
		sortedProductIds.sort(Long::compareTo);

		List<Product> lockedProducts = new ArrayList<>();

		for (Long productId : sortedProductIds) {

			Product product = productRepository.findByIdForUpdate(productId)
					.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

			lockedProducts.add(product);
		}

		/*
		 * All product rows are now locked for this transaction. Validate stock and
		 * calculate the sale total.
		 */
		Sale sale = new Sale();

		sale.setInvoiceNumber(generateInvoiceNumber());
		sale.setPaymentMethod(request.getPaymentMethod());
		sale.setStatus(Sale.SaleStatus.COMPLETED);
		sale.setTotalAmount(BigDecimal.ZERO);

		BigDecimal total = BigDecimal.ZERO;

		for (SaleRequest.SaleItemRequest itemRequest : request.getItems()) {

			Product product = lockedProducts.stream().filter(p -> p.getId().equals(itemRequest.getProductId()))
					.findFirst().orElseThrow(() -> new ResourceNotFoundException(
							"Product not found with ID: " + itemRequest.getProductId()));

			int requestedQuantity = itemRequest.getQuantity();

			if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {

				throw new BadRequestException("Product has an invalid price: " + product.getName());
			}

			if (product.getQuantity() == null || product.getQuantity() < requestedQuantity) {

				throw new BadRequestException("Insufficient stock for product: " + product.getName()
						+ ". Available quantity: " + product.getQuantity());
			}

			SaleItem saleItem = new SaleItem();

			saleItem.setProduct(product);
			saleItem.setQuantity(requestedQuantity);
			saleItem.setUnitPrice(product.getPrice());
			saleItem.calculateSubtotal();

			sale.addItem(saleItem);

			total = total.add(saleItem.getSubtotal());
		}

		sale.setTotalAmount(total);

		// Save the sale and its items in the current transaction.
		Sale savedSale = saleRepository.saveAndFlush(sale);

		/*
		 * Deduct stock from the same locked Product entities. No second product lookup
		 * is necessary.
		 */
		for (SaleRequest.SaleItemRequest itemRequest : request.getItems()) {

			Product product = lockedProducts.stream().filter(p -> p.getId().equals(itemRequest.getProductId()))
					.findFirst().orElseThrow(() -> new ResourceNotFoundException(
							"Product not found with ID: " + itemRequest.getProductId()));

			product.setQuantity(product.getQuantity() - itemRequest.getQuantity());

			productRepository.save(product);
		}

		// Record the completed sale in the audit log.
		auditLogService.recordAction("SALE_CREATED", "Sale", savedSale.getId(),
				"Invoice: " + savedSale.getInvoiceNumber() + "; Total amount: " + savedSale.getTotalAmount()
						+ "; Payment method: " + savedSale.getPaymentMethod() + "; Status: " + savedSale.getStatus());

		return new SaleResponse(savedSale);
	}

// Retrieve all sales.
	@Transactional(readOnly = true)
	public List<SaleResponse> getAllSales() {

		return saleRepository.findAll().stream().map(SaleResponse::new).toList();
	}

// Retrieve a sale by ID.
	@Transactional(readOnly = true)
	public SaleResponse getSaleById(Long id) {

		return new SaleResponse(findSaleById(id));
	}

// Retrieve a sale by invoice number.
	@Transactional(readOnly = true)
	public SaleResponse getSaleByInvoiceNumber(String invoiceNumber) {

		if (invoiceNumber == null || invoiceNumber.isBlank()) {
			throw new BadRequestException("Invoice number must not be empty.");
		}

		String normalizedInvoiceNumber = invoiceNumber.trim();

		Sale sale = saleRepository.findByInvoiceNumber(normalizedInvoiceNumber).orElseThrow(
				() -> new ResourceNotFoundException("Sale not found with invoice number: " + normalizedInvoiceNumber));

		return new SaleResponse(sale);
	}

// Retrieve sales within a date range.
	@Transactional(readOnly = true)
	public List<SaleResponse> getSalesBetween(LocalDateTime startDate, LocalDateTime endDate) {

		if (startDate == null || endDate == null) {
			throw new BadRequestException("Start date and end date are required.");
		}

		if (startDate.isAfter(endDate)) {
			throw new BadRequestException("Start date must not be after end date.");
		}

		return saleRepository.findBySaleDateBetween(startDate, endDate).stream().map(SaleResponse::new).toList();
	}

	private Sale findSaleById(Long id) {

		if (id == null || id <= 0) {
			throw new BadRequestException("Sale ID must be a positive number.");
		}

		return saleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Sale not found with ID: " + id));
	}

	private String generateInvoiceNumber() {

		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);

		return "FNX-" + timestamp + "-" + suffix;
	}

}
