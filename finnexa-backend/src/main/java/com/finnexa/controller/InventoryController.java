package com.finnexa.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finnexa.dto.InventoryResponse;
import com.finnexa.dto.InventorySummaryResponse;
import com.finnexa.service.InventoryService;
import com.finnexa.service.InventorySummaryService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
	private final InventoryService inventoryService;
	private final InventorySummaryService inventorySummaryService;

	public InventoryController(InventoryService inventoryService, InventorySummaryService inventorySummaryService) {
		this.inventoryService = inventoryService;
		this.inventorySummaryService = inventorySummaryService;
	}

	@GetMapping
	public ResponseEntity<List<InventoryResponse>> getAllInventory() {
		return ResponseEntity.ok(inventoryService.getAllInventory());
	}

	@GetMapping("/{productId}")
	public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
		return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
	}

	@GetMapping("/low-stock")
	public ResponseEntity<List<InventoryResponse>> getLowStockInventory() {
		return ResponseEntity.ok(inventoryService.getLowStockInventory());
	}

	@GetMapping("/summary")
	public ResponseEntity<InventorySummaryResponse> getInventorySummary() {
		return ResponseEntity.ok(inventorySummaryService.getInventorySummary());
	}

	@GetMapping("/total-value")
	public ResponseEntity<BigDecimal> getTotalInventorySellingValue() {
		return ResponseEntity.ok(inventoryService.getTotalInventorySellingValue());
	}

	@GetMapping("/count")
	public ResponseEntity<Long> getTotalProductCount() {
		return ResponseEntity.ok(inventoryService.getTotalProductCount());
	}

	@GetMapping("/low-stock/count")
	public ResponseEntity<Long> getLowStockProductCount() {
		return ResponseEntity.ok(inventoryService.getLowStockProductCount());
	}
}