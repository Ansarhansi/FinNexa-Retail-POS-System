package com.finnexa.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.ProductRequest;
import com.finnexa.dto.ProductResponse;
import com.finnexa.entity.Product;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

	private final ProductRepository productRepository;
	private final AuditLogService auditLogService;

	public ProductService(ProductRepository productRepository, AuditLogService auditLogService) {

		this.productRepository = productRepository;
		this.auditLogService = auditLogService;
	}

	// Create a new product
	public ProductResponse createProduct(ProductRequest request) {

		if (request == null) {
			throw new BadRequestException("Product request is required.");
		}

		String sku = normalizeSku(request.getSku());

		if (productRepository.existsBySkuIgnoreCase(sku)) {
			throw new BadRequestException("A product with SKU '" + sku + "' already exists.");
		}

		if (request.getName() == null || request.getName().isBlank()) {

			throw new BadRequestException("Product name must not be empty.");
		}

		if (request.getPrice() == null || request.getPrice().signum() < 0) {

			throw new BadRequestException("Product price must be zero or greater.");
		}

		if (request.getQuantity() == null || request.getQuantity() < 0) {

			throw new BadRequestException("Product quantity must be zero or greater.");
		}

		Product product = new Product();

		product.setName(request.getName().trim());
		product.setSku(sku);
		product.setDescription(normalizeDescription(request.getDescription()));
		product.setPrice(request.getPrice());
		product.setQuantity(request.getQuantity());

		Product savedProduct = productRepository.save(product);

		auditLogService.recordAction("PRODUCT_CREATED", "Product", savedProduct.getId(),
				"Created product: " + savedProduct.getName() + "; SKU: " + savedProduct.getSku() + "; Price: "
						+ savedProduct.getPrice() + "; Initial quantity: " + savedProduct.getQuantity());

		return new ProductResponse(savedProduct);
	}

	// Retrieve all products (existing API)
	@Transactional(readOnly = true)
	public List<ProductResponse> getAllProducts() {

		return productRepository.findAll().stream().map(ProductResponse::new).toList();
	}

	// Retrieve products with pagination and sorting
	@Transactional(readOnly = true)
	public Page<ProductResponse> getProducts(int page, int size, String sortBy, String direction) {

		if (page < 0) {
			throw new BadRequestException("Page number must be zero or greater.");
		}

		if (size < 1 || size > 100) {
			throw new BadRequestException("Page size must be between 1 and 100.");
		}

		if (sortBy == null || sortBy.isBlank()) {
			throw new BadRequestException("Sort field must not be empty.");
		}

		List<String> allowedSortFields = List.of("id", "name", "sku", "price", "quantity");

		if (!allowedSortFields.contains(sortBy)) {
			throw new BadRequestException(
					"Invalid sort field. Allowed fields: " + String.join(", ", allowedSortFields));
		}

		if (direction == null || (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc"))) {

			throw new BadRequestException("Sort direction must be 'asc' or 'desc'.");
		}

		Sort.Direction sortDirection = Sort.Direction.fromString(direction);

		Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

		return productRepository.findAll(pageable).map(ProductResponse::new);
	}

	// Retrieve a product by ID
	@Transactional(readOnly = true)
	public ProductResponse getProductById(Long id) {

		Product product = findProductById(id);

		return new ProductResponse(product);
	}

	// Retrieve a product by SKU
	@Transactional(readOnly = true)
	public ProductResponse getProductBySku(String sku) {

		String normalizedSku = normalizeSku(sku);

		Product product = productRepository.findBySkuIgnoreCase(normalizedSku)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + normalizedSku));

		return new ProductResponse(product);
	}

	// Search products by name
	@Transactional(readOnly = true)
	public List<ProductResponse> searchProducts(String name) {

		if (name == null || name.isBlank()) {
			throw new BadRequestException("Search name must not be empty.");
		}

		return productRepository.findByNameContainingIgnoreCase(name.trim()).stream().map(ProductResponse::new)
				.toList();
	}

	// Find products at or below a stock threshold
	@Transactional(readOnly = true)
	public List<ProductResponse> getLowStockProducts(Integer threshold) {

		if (threshold == null || threshold < 0) {
			throw new BadRequestException("Stock threshold must be zero or greater.");
		}

		return productRepository.findByQuantityLessThanEqual(threshold).stream().map(ProductResponse::new).toList();
	}

	// Update an existing product
	public ProductResponse updateProduct(Long id, ProductRequest request) {

		if (request == null) {
			throw new BadRequestException("Product request is required.");
		}

		if (request.getName() == null || request.getName().isBlank()) {

			throw new BadRequestException("Product name must not be empty.");
		}

		if (request.getPrice() == null || request.getPrice().signum() < 0) {

			throw new BadRequestException("Product price must be zero or greater.");
		}

		if (request.getQuantity() == null || request.getQuantity() < 0) {

			throw new BadRequestException("Product quantity must be zero or greater.");
		}

		Product product = findProductById(id);

		String previousName = product.getName();
		String previousSku = product.getSku();

		String newSku = normalizeSku(request.getSku());

		boolean skuChanged = !previousSku.equalsIgnoreCase(newSku);

		if (skuChanged && productRepository.existsBySkuIgnoreCase(newSku)) {

			throw new BadRequestException("Another product already uses SKU '" + newSku + "'.");
		}

		product.setName(request.getName().trim());
		product.setSku(newSku);
		product.setDescription(normalizeDescription(request.getDescription()));
		product.setPrice(request.getPrice());
		product.setQuantity(request.getQuantity());

		Product updatedProduct = productRepository.save(product);

		auditLogService.recordAction("PRODUCT_UPDATED", "Product", updatedProduct.getId(),
				"Updated product. Previous name: " + previousName + "; Current name: " + updatedProduct.getName()
						+ "; Previous SKU: " + previousSku + "; Current SKU: " + updatedProduct.getSku());

		return new ProductResponse(updatedProduct);
	}

	// Delete an existing product
	public void deleteProduct(Long id) {

		Product product = findProductById(id);

		Long productId = product.getId();
		String productName = product.getName();
		String sku = product.getSku();

		auditLogService.recordAction("PRODUCT_DELETED", "Product", productId,
				"Deleted product: " + productName + "; SKU: " + sku);

		productRepository.delete(product);
	}

	// Find a product or throw an exception
	private Product findProductById(Long id) {

		if (id == null || id <= 0) {
			throw new BadRequestException("Product ID must be a positive number.");
		}

		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
	}

	// Normalize SKU
	private String normalizeSku(String sku) {

		if (sku == null || sku.isBlank()) {
			throw new BadRequestException("SKU must not be empty.");
		}

		return sku.trim().toUpperCase(Locale.ROOT);
	}

	// Normalize optional descriptions
	private String normalizeDescription(String description) {

		if (description == null || description.isBlank()) {
			return null;
		}

		return description.trim();
	}
}