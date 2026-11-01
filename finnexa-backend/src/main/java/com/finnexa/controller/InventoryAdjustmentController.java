package com.finnexa.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finnexa.dto.InventoryAdjustmentRequest;
import com.finnexa.dto.InventoryAdjustmentResponse;
import com.finnexa.service.InventoryAdjustmentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory/adjustments")
public class InventoryAdjustmentController {
	private final InventoryAdjustmentService inventoryAdjustmentService;

	public InventoryAdjustmentController(InventoryAdjustmentService inventoryAdjustmentService) {
		this.inventoryAdjustmentService = inventoryAdjustmentService;
	}

	@PostMapping
	public ResponseEntity<InventoryAdjustmentResponse> adjustStock(
			@Valid @RequestBody InventoryAdjustmentRequest request) {
		InventoryAdjustmentResponse response = inventoryAdjustmentService.adjustStock(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<InventoryAdjustmentResponse>> getAllAdjustments() {
		return ResponseEntity.ok(inventoryAdjustmentService.getAllAdjustments());
	}

	@GetMapping("/product/{productId}")
	public ResponseEntity<List<InventoryAdjustmentResponse>> getAdjustmentsByProductId(@PathVariable Long productId) {
		return ResponseEntity.ok(inventoryAdjustmentService.getAdjustmentsByProductId(productId));
	}
}
