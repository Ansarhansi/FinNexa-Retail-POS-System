package com.finnexa.controller;

import java.util.List;

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

import com.finnexa.dto.SupplierRequest;
import com.finnexa.dto.SupplierResponse;
import com.finnexa.service.SupplierService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

// Create a supplier
	@PostMapping
	public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {

		SupplierResponse response = supplierService.createSupplier(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

// Get all suppliers
	@GetMapping
	public ResponseEntity<List<SupplierResponse>> getAllSuppliers() {
		return ResponseEntity.ok(supplierService.getAllSuppliers());
	}

// Get supplier by ID
	@GetMapping("/{id}")
	public ResponseEntity<SupplierResponse> getSupplierById(@PathVariable Long id) {

		return ResponseEntity.ok(supplierService.getSupplierById(id));
	}

// Get supplier by email
	@GetMapping("/email")
	public ResponseEntity<SupplierResponse> getSupplierByEmail(@RequestParam String email) {

		return ResponseEntity.ok(supplierService.getSupplierByEmail(email));
	}

// Search suppliers by name
	@GetMapping("/search")
	public ResponseEntity<List<SupplierResponse>> searchSuppliers(@RequestParam String name) {

		return ResponseEntity.ok(supplierService.searchSuppliers(name));
	}

// Search by contact person
	@GetMapping("/search/contact")
	public ResponseEntity<List<SupplierResponse>> searchByContactPerson(@RequestParam String name) {

		return ResponseEntity.ok(supplierService.searchByContactPerson(name));
	}

// Update supplier
	@PutMapping("/{id}")
	public ResponseEntity<SupplierResponse> updateSupplier(@PathVariable Long id,
			@Valid @RequestBody SupplierRequest request) {

		return ResponseEntity.ok(supplierService.updateSupplier(id, request));
	}

// Delete supplier
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {

		supplierService.deleteSupplier(id);

		return ResponseEntity.noContent().build();
	}

}