package com.finnexa.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finnexa.dto.InventoryResponse;
import com.finnexa.entity.Product;
import com.finnexa.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class InventoryService {
	private static final int LOW_STOCK_THRESHOLD = 5;
	private final ProductRepository productRepository;

	public InventoryService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public List<InventoryResponse> getAllInventory() {
		return productRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	public InventoryResponse getInventoryByProductId(Long productId) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
		return mapToResponse(product);
	}

	public List<InventoryResponse> getLowStockInventory() {
		return productRepository.findByQuantityLessThanEqual(LOW_STOCK_THRESHOLD).stream().map(this::mapToResponse)
				.toList();
	}

	public long getTotalProductCount() {
		return productRepository.count();
	}

	public long getLowStockProductCount() {
		return productRepository.findByQuantityLessThanEqual(LOW_STOCK_THRESHOLD).size();
	}

	public BigDecimal getTotalInventorySellingValue() {
		return productRepository.findAll().stream()
				.map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private InventoryResponse mapToResponse(Product product) {
		boolean lowStock = product.getQuantity() <= LOW_STOCK_THRESHOLD;
		return new InventoryResponse(product.getId(), product.getName(), product.getSku(), product.getQuantity(),
				product.getPrice(), lowStock);
	}
}