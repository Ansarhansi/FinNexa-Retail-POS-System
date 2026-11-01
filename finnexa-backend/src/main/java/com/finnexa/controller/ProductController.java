package com.finnexa.controller;

import java.util.List;
import org.springframework.data.domain.Page;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finnexa.dto.ProductRequest;
import com.finnexa.dto.ProductResponse;
import com.finnexa.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {

		ProductResponse response = productService.createProduct(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ProductResponse>> getAllProducts() {
		return ResponseEntity.ok(productService.getAllProducts());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {

		return ResponseEntity.ok(productService.getProductById(id));
	}

	@GetMapping("/sku/{sku}")
	public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {

		return ResponseEntity.ok(productService.getProductBySku(sku));
	}

	@GetMapping("/search")
	public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String name) {

		return ResponseEntity.ok(productService.searchProducts(name));
	}

	@GetMapping("/low-stock")
	public ResponseEntity<List<ProductResponse>> getLowStockProducts(
			@RequestParam(defaultValue = "10") Integer threshold) {

		return ResponseEntity.ok(productService.getLowStockProducts(threshold));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
			@Valid @RequestBody ProductRequest request) {

		return ResponseEntity.ok(productService.updateProduct(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
		productService.deleteProduct(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/page")
	public ResponseEntity<Page<ProductResponse>> getProducts(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String direction) {

		return ResponseEntity.ok(productService.getProducts(page, size, sortBy, direction));
	}

}