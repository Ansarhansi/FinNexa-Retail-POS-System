package com.finnexa.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finnexa.dto.PurchaseRequest;
import com.finnexa.dto.PurchaseResponse;
import com.finnexa.service.PurchaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

	private final PurchaseService purchaseService;

	public PurchaseController(PurchaseService purchaseService) {
		this.purchaseService = purchaseService;
	}

// Create a purchase order.
	@PostMapping
	public ResponseEntity<PurchaseResponse> createPurchase(@Valid @RequestBody PurchaseRequest request) {

		PurchaseResponse response = purchaseService.createPurchase(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

// Retrieve all purchases.
	@GetMapping
	public ResponseEntity<List<PurchaseResponse>> getAllPurchases() {
		return ResponseEntity.ok(purchaseService.getAllPurchases());
	}

// Retrieve a purchase by ID.
	@GetMapping("/{id}")
	public ResponseEntity<PurchaseResponse> getPurchaseById(@PathVariable Long id) {

		return ResponseEntity.ok(purchaseService.getPurchaseById(id));
	}

// Retrieve a purchase by purchase number.
	@GetMapping("/number")
	public ResponseEntity<PurchaseResponse> getPurchaseByNumber(@RequestParam String purchaseNumber) {

		return ResponseEntity.ok(purchaseService.getPurchaseByNumber(purchaseNumber));
	}

// Retrieve purchases for a supplier.
	@GetMapping("/supplier/{supplierId}")
	public ResponseEntity<List<PurchaseResponse>> getPurchasesBySupplier(@PathVariable Long supplierId) {

		return ResponseEntity.ok(purchaseService.getPurchasesBySupplier(supplierId));
	}

// Mark a pending purchase as received and increase stock.
	@PostMapping("/{id}/receive")
	public ResponseEntity<PurchaseResponse> receivePurchase(@PathVariable Long id) {

		return ResponseEntity.ok(purchaseService.receivePurchase(id));
	}

// Cancel a pending purchase.
	@PostMapping("/{id}/cancel")
	public ResponseEntity<PurchaseResponse> cancelPurchase(@PathVariable Long id) {

		return ResponseEntity.ok(purchaseService.cancelPurchase(id));
	}

}