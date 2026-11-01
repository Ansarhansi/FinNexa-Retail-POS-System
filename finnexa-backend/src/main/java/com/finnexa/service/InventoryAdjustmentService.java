
package com.finnexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.InventoryAdjustmentRequest;
import com.finnexa.dto.InventoryAdjustmentResponse;
import com.finnexa.entity.InventoryAdjustment;
import com.finnexa.entity.Product;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.InventoryAdjustmentRepository;
import com.finnexa.repository.ProductRepository;

@Service
public class InventoryAdjustmentService {

	private final InventoryAdjustmentRepository adjustmentRepository;
	private final ProductRepository productRepository;
	private final AuditLogService auditLogService;

	public InventoryAdjustmentService(InventoryAdjustmentRepository adjustmentRepository,
			ProductRepository productRepository, AuditLogService auditLogService) {

		this.adjustmentRepository = adjustmentRepository;
		this.productRepository = productRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public InventoryAdjustmentResponse adjustStock(InventoryAdjustmentRequest request) {

		// Validate the request.
		if (request == null || request.getProductId() == null || request.getQuantityChange() == null
				|| request.getReason() == null || request.getReason().isBlank()) {

			throw new BadRequestException("Product ID, quantity change, and reason are required.");
		}

		if (request.getProductId() <= 0) {
			throw new BadRequestException("Product ID must be greater than zero.");
		}

		if (request.getQuantityChange() == 0) {
			throw new BadRequestException("Quantity change cannot be zero.");
		}

		// Lock the product row before reading or modifying stock.
		Product product = productRepository.findByIdForUpdate(request.getProductId()).orElseThrow(
				() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

		// Validate the existing stock quantity.
		Integer currentQuantity = product.getQuantity();

		if (currentQuantity == null || currentQuantity < 0) {
			throw new BadRequestException("Product has an invalid stock quantity: " + product.getName());
		}

		int previousQuantity = currentQuantity;
		int quantityChange = request.getQuantityChange();

		// Calculate using long to prevent integer overflow.
		long calculatedQuantity = (long) previousQuantity + quantityChange;

		if (calculatedQuantity < 0) {
			throw new BadRequestException("Stock adjustment cannot result in negative inventory.");
		}

		if (calculatedQuantity > Integer.MAX_VALUE) {
			throw new BadRequestException("Resulting stock quantity is too large.");
		}

		int newQuantity = (int) calculatedQuantity;
		String reason = request.getReason().trim();

		// Update inventory.
		product.setQuantity(newQuantity);
		productRepository.save(product);

		// Record the stock adjustment history.
		InventoryAdjustment adjustment = new InventoryAdjustment();
		adjustment.setProduct(product);
		adjustment.setQuantityChange(quantityChange);
		adjustment.setPreviousQuantity(previousQuantity);
		adjustment.setNewQuantity(newQuantity);
		adjustment.setReason(reason);

		InventoryAdjustment savedAdjustment = adjustmentRepository.save(adjustment);

		// Record the action in the audit log.
		auditLogService.recordAction("STOCK_ADJUSTMENT", "Product", product.getId(),
				"Product: " + product.getName() + "; Previous quantity: " + previousQuantity + "; Quantity change: "
						+ quantityChange + "; New quantity: " + newQuantity + "; Reason: " + reason);

		return new InventoryAdjustmentResponse(savedAdjustment.getId(), product.getId(), product.getName(),
				quantityChange, previousQuantity, newQuantity, savedAdjustment.getReason(),
				savedAdjustment.getAdjustedAt());
	}

	@Transactional(readOnly = true)
	public List<InventoryAdjustmentResponse> getAllAdjustments() {

		return adjustmentRepository.findAllByOrderByAdjustedAtDesc().stream().map(this::mapToResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<InventoryAdjustmentResponse> getAdjustmentsByProductId(Long productId) {

		if (productId == null || productId <= 0) {
			throw new BadRequestException("Product ID must be a positive number.");
		}

		if (!productRepository.existsById(productId)) {
			throw new ResourceNotFoundException("Product not found with ID: " + productId);
		}

		return adjustmentRepository.findByProductIdOrderByAdjustedAtDesc(productId).stream().map(this::mapToResponse)
				.toList();
	}

	private InventoryAdjustmentResponse mapToResponse(InventoryAdjustment adjustment) {

		Product product = adjustment.getProduct();

		Integer quantityChange = adjustment.getQuantityChange();
		Integer previousQuantity = adjustment.getPreviousQuantity();
		Integer newQuantity = adjustment.getNewQuantity();

		// Compatibility fallback for legacy records.
		// Prefer migrating legacy records to preserve historical accuracy.
		if (previousQuantity == null || newQuantity == null) {

			if (quantityChange == null || product.getQuantity() == null) {
				throw new IllegalStateException(
						"Cannot reconstruct quantities for inventory adjustment ID: " + adjustment.getId());
			}

			newQuantity = product.getQuantity();
			previousQuantity = newQuantity - quantityChange;
		}

		return new InventoryAdjustmentResponse(adjustment.getId(), product.getId(), product.getName(), quantityChange,
				previousQuantity, newQuantity, adjustment.getReason(), adjustment.getAdjustedAt());
	}
}
