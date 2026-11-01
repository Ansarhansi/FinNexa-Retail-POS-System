package com.finnexa.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finnexa.dto.InventorySummaryResponse;
import com.finnexa.entity.Product;
import com.finnexa.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class InventorySummaryService {
	private static final int LOW_STOCK_THRESHOLD = 5;
	private final ProductRepository productRepository;

	public InventorySummaryService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public InventorySummaryResponse getInventorySummary() {
		List<Product> products = productRepository.findAll();
		long totalProducts = products.size();
		long lowStockProducts = products.stream()
				.filter(product -> product.getQuantity() > 0 && product.getQuantity() <= LOW_STOCK_THRESHOLD).count();
		long outOfStockProducts = products.stream().filter(product -> product.getQuantity() == 0).count();
		BigDecimal totalInventorySellingValue = products.stream()
				.map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new InventorySummaryResponse(totalProducts, lowStockProducts, outOfStockProducts,
				totalInventorySellingValue);
	}
}