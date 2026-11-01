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

import com.finnexa.dto.CustomerRequest;
import com.finnexa.dto.CustomerResponse;
import com.finnexa.service.CustomerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

// Create a customer
	@PostMapping
	public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {

		CustomerResponse response = customerService.createCustomer(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

// Get all customers
	@GetMapping
	public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
		return ResponseEntity.ok(customerService.getAllCustomers());
	}

// Get customer by ID
	@GetMapping("/{id}")
	public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {

		return ResponseEntity.ok(customerService.getCustomerById(id));
	}

// Get customer by email
	@GetMapping("/email")
	public ResponseEntity<CustomerResponse> getCustomerByEmail(@RequestParam String email) {

		return ResponseEntity.ok(customerService.getCustomerByEmail(email));
	}

// Search customers by name
	@GetMapping("/search")
	public ResponseEntity<List<CustomerResponse>> searchCustomers(@RequestParam String name) {

		return ResponseEntity.ok(customerService.searchCustomers(name));
	}

// Update customer
	@PutMapping("/{id}")
	public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id,
			@Valid @RequestBody CustomerRequest request) {

		return ResponseEntity.ok(customerService.updateCustomer(id, request));
	}

// Delete customer
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
		customerService.deleteCustomer(id);

		return ResponseEntity.noContent().build();
	}

}