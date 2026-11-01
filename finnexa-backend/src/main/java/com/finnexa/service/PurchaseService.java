package com.finnexa.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional
public class PurchaseService {

	private final PurchaseRepository purchaseRepository;
	private final ProductRepository productRepository;
	private final SupplierRepository supplierRepository;
	private final AuditLogService auditLogService;

	public PurchaseService(PurchaseRepository purchaseRepository, ProductRepository productRepository,
			SupplierRepository supplierRepository, AuditLogService auditLogService) {
		this.purchaseRepository = purchaseRepository;
		this.productRepository = productRepository;
		this.supplierRepository = supplierRepository;
		this.auditLogService = auditLogService;
	}

	// Create a purchase order. Stock is not increased until the purchase is
	// received.
	public PurchaseResponse createPurchase(PurchaseRequest request) {
		if (request == null || request.getSupplierId() == null || request.getSupplierId() <= 0) {
			throw new BadRequestException("A valid supplier ID is required.");
		}
		if (request.getItems() == null || request.getItems().isEmpty()) {
			throw new BadRequestException("A purchase must contain at least one item.");
		}

		Set<Long> productIds = new HashSet<>();
		for (PurchaseRequest.PurchaseItemRequest item : request.getItems()) {
			if (item == null || item.getProductId() == null || item.getProductId() <= 0) {
				throw new BadRequestException("Every purchase item must have a valid product ID.");
			}
			if (item.getQuantity() == null || item.getQuantity() <= 0) {
				throw new BadRequestException("Purchase quantity must be greater than zero.");
			}
			if (item.getUnitCost() == null || item.getUnitCost().compareTo(BigDecimal.ZERO) <= 0) {
				throw new BadRequestException("Purchase unit cost must be greater than zero.");
			}
			if (!productIds.add(item.getProductId())) {
				throw new BadRequestException("A product cannot appear more than once in a purchase. "
						+ "Combine its quantities into one item.");
			}
		}

		Supplier supplier = supplierRepository.findById(request.getSupplierId()).orElseThrow(
				() -> new ResourceNotFoundException("Supplier not found with ID: " + request.getSupplierId()));

		Purchase purchase = new Purchase();
		purchase.setPurchaseNumber(generatePurchaseNumber());
		purchase.setSupplier(supplier);
		purchase.setStatus(Purchase.PurchaseStatus.PENDING);
		purchase.setTotalAmount(BigDecimal.ZERO);

		BigDecimal total = BigDecimal.ZERO;
		for (PurchaseRequest.PurchaseItemRequest itemRequest : request.getItems()) {
			Product product = productRepository.findById(itemRequest.getProductId()).orElseThrow(
					() -> new ResourceNotFoundException("Product not found with ID: " + itemRequest.getProductId()));

			PurchaseItem item = new PurchaseItem();
			item.setProduct(product);
			item.setQuantity(itemRequest.getQuantity());
			item.setUnitCost(itemRequest.getUnitCost());
			item.calculateSubtotal();
			purchase.addItem(item);
			total = total.add(item.getSubtotal());
		}

		purchase.setTotalAmount(total);
		Purchase savedPurchase = purchaseRepository.save(purchase);
		auditLogService.recordAction("PURCHASE_CREATED", "Purchase", savedPurchase.getId(),
				"Purchase number: " + savedPurchase.getPurchaseNumber() + "; Supplier: " + supplier.getName()
						+ "; Total amount: " + savedPurchase.getTotalAmount() + "; Status: "
						+ savedPurchase.getStatus());
		return new PurchaseResponse(savedPurchase);
	}

	@Transactional(readOnly = true)
	public List<PurchaseResponse> getAllPurchases() {
		return purchaseRepository.findAll().stream().map(PurchaseResponse::new).toList();
	}

	@Transactional(readOnly = true)
	public PurchaseResponse getPurchaseById(Long id) {
		return new PurchaseResponse(findPurchaseById(id));
	}

	@Transactional(readOnly = true)
	public PurchaseResponse getPurchaseByNumber(String purchaseNumber) {
		if (purchaseNumber == null || purchaseNumber.isBlank()) {
			throw new BadRequestException("Purchase number must not be empty.");
		}
		Purchase purchase = purchaseRepository.findByPurchaseNumber(purchaseNumber.trim())
				.orElseThrow(() -> new ResourceNotFoundException("Purchase not found with number: " + purchaseNumber));
		return new PurchaseResponse(purchase);
	}

	@Transactional(readOnly = true)
	public List<PurchaseResponse> getPurchasesBySupplier(Long supplierId) {
		if (supplierId == null || supplierId <= 0) {
			throw new BadRequestException("Supplier ID must be a positive number.");
		}
		if (!supplierRepository.existsById(supplierId)) {
			throw new ResourceNotFoundException("Supplier not found with ID: " + supplierId);
		}
		return purchaseRepository.findBySupplierId(supplierId).stream().map(PurchaseResponse::new).toList();
	}

	// Lock the purchase first, then lock its products in ascending ID order.
	// The transaction ensures stock and status changes roll back together on
	// failure.
	public PurchaseResponse receivePurchase(Long id) {
		Purchase purchase = findPurchaseByIdForUpdate(id);
		if (purchase.getStatus() != Purchase.PurchaseStatus.PENDING) {
			throw new BadRequestException("Only pending purchases can be received.");
		}

		List<PurchaseItem> items = new ArrayList<>(purchase.getItems());
		items.sort(Comparator.comparing(item -> item.getProduct().getId()));

		Map<Long, Product> lockedProducts = new HashMap<>();
		for (PurchaseItem item : items) {
			Long productId = item.getProduct().getId();
			Product lockedProduct = productRepository.findByIdForUpdate(productId)
					.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
			if (lockedProduct.getQuantity() == null || lockedProduct.getQuantity() < 0) {
				throw new BadRequestException("Product has an invalid stock quantity: " + lockedProduct.getName());
			}
			try {
				Math.addExact(lockedProduct.getQuantity(), item.getQuantity());
			} catch (ArithmeticException ex) {
				throw new BadRequestException("Stock quantity overflow for product: " + lockedProduct.getName());
			}
			lockedProducts.put(productId, lockedProduct);
		}

		// All products have been locked and validated before any quantity is changed.
		for (PurchaseItem item : items) {
			Product product = lockedProducts.get(item.getProduct().getId());
			product.setQuantity(Math.addExact(product.getQuantity(), item.getQuantity()));
			productRepository.save(product);
		}

		purchase.setStatus(Purchase.PurchaseStatus.RECEIVED);
		Purchase updatedPurchase = purchaseRepository.save(purchase);
		auditLogService.recordAction("PURCHASE_RECEIVED", "Purchase", updatedPurchase.getId(),
				"Purchase number: " + updatedPurchase.getPurchaseNumber() + "; Total amount: "
						+ updatedPurchase.getTotalAmount() + "; Status: " + updatedPurchase.getStatus()
						+ "; Inventory stock increased.");
		return new PurchaseResponse(updatedPurchase);
	}

	// Cancellation is only allowed while pending and is serialized against
	// receiving.
	public PurchaseResponse cancelPurchase(Long id) {
		Purchase purchase = findPurchaseByIdForUpdate(id);
		if (purchase.getStatus() != Purchase.PurchaseStatus.PENDING) {
			throw new BadRequestException("Only pending purchases can be cancelled.");
		}
		purchase.setStatus(Purchase.PurchaseStatus.CANCELLED);
		Purchase cancelledPurchase = purchaseRepository.save(purchase);
		auditLogService.recordAction("PURCHASE_CANCELLED", "Purchase", cancelledPurchase.getId(),
				"Purchase number: " + cancelledPurchase.getPurchaseNumber() + "; Total amount: "
						+ cancelledPurchase.getTotalAmount() + "; Status: " + cancelledPurchase.getStatus());
		return new PurchaseResponse(cancelledPurchase);
	}

	private Purchase findPurchaseById(Long id) {
		validatePurchaseId(id);
		return purchaseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Purchase not found with ID: " + id));
	}

	private Purchase findPurchaseByIdForUpdate(Long id) {
		validatePurchaseId(id);
		return purchaseRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Purchase not found with ID: " + id));
	}

	private void validatePurchaseId(Long id) {
		if (id == null || id <= 0) {
			throw new BadRequestException("Purchase ID must be a positive number.");
		}
	}

	private String generatePurchaseNumber() {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
		return "PUR-" + timestamp + "-" + suffix;
	}
}
