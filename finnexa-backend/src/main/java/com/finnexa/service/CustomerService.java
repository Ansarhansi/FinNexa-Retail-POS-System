
package com.finnexa.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.CustomerRequest;
import com.finnexa.dto.CustomerResponse;
import com.finnexa.entity.Customer;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.CustomerRepository;

@Service
@Transactional
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final AuditLogService auditLogService;

	public CustomerService(CustomerRepository customerRepository, AuditLogService auditLogService) {

		this.customerRepository = customerRepository;
		this.auditLogService = auditLogService;
	}

	// Create a customer
	public CustomerResponse createCustomer(CustomerRequest request) {

		if (request == null) {
			throw new BadRequestException("Customer request must not be empty.");
		}

		if (request.getName() == null || request.getName().isBlank()) {
			throw new BadRequestException("Customer name must not be empty.");
		}

		Customer customer = new Customer();

		customer.setName(request.getName().trim());
		customer.setEmail(normalizeEmail(request.getEmail()));
		customer.setPhone(normalizeOptionalText(request.getPhone()));
		customer.setAddress(normalizeOptionalText(request.getAddress()));

		Customer savedCustomer = customerRepository.save(customer);

		auditLogService.recordAction("CUSTOMER_CREATED", "Customer", savedCustomer.getId(),
				"Customer created: " + savedCustomer.getName());

		return new CustomerResponse(savedCustomer);
	}

	// Retrieve all customers
	@Transactional(readOnly = true)
	public List<CustomerResponse> getAllCustomers() {

		return customerRepository.findAll().stream().map(CustomerResponse::new).toList();
	}

	// Retrieve a customer by ID
	@Transactional(readOnly = true)
	public CustomerResponse getCustomerById(Long id) {

		return new CustomerResponse(findCustomerById(id));
	}

	// Retrieve a customer by email
	@Transactional(readOnly = true)
	public CustomerResponse getCustomerByEmail(String email) {

		String normalizedEmail = normalizeEmail(email);

		if (normalizedEmail == null) {
			throw new BadRequestException("Email must not be empty.");
		}

		Customer customer = customerRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + normalizedEmail));

		return new CustomerResponse(customer);
	}

	// Search customers by name
	@Transactional(readOnly = true)
	public List<CustomerResponse> searchCustomers(String name) {

		if (name == null || name.isBlank()) {
			throw new BadRequestException("Search name must not be empty.");
		}

		return customerRepository.findByNameContainingIgnoreCase(name.trim()).stream().map(CustomerResponse::new)
				.toList();
	}

	// Update a customer
	public CustomerResponse updateCustomer(Long id, CustomerRequest request) {

		if (request == null) {
			throw new BadRequestException("Customer request must not be empty.");
		}

		if (request.getName() == null || request.getName().isBlank()) {
			throw new BadRequestException("Customer name must not be empty.");
		}

		Customer customer = findCustomerById(id);

		customer.setName(request.getName().trim());
		customer.setEmail(normalizeEmail(request.getEmail()));
		customer.setPhone(normalizeOptionalText(request.getPhone()));
		customer.setAddress(normalizeOptionalText(request.getAddress()));

		Customer updatedCustomer = customerRepository.save(customer);

		auditLogService.recordAction("CUSTOMER_UPDATED", "Customer", updatedCustomer.getId(),
				"Customer updated: " + updatedCustomer.getName());

		return new CustomerResponse(updatedCustomer);
	}

	// Delete a customer
	public void deleteCustomer(Long id) {

		Customer customer = findCustomerById(id);

		Long customerId = customer.getId();
		String customerName = customer.getName();

		customerRepository.delete(customer);

		auditLogService.recordAction("CUSTOMER_DELETED", "Customer", customerId, "Customer deleted: " + customerName);
	}

	// Find customer by ID
	private Customer findCustomerById(Long id) {

		if (id == null || id <= 0) {
			throw new BadRequestException("Customer ID must be a positive number.");
		}

		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
	}

	// Normalize email
	private String normalizeEmail(String email) {

		if (email == null || email.isBlank()) {
			return null;
		}

		return email.trim().toLowerCase(Locale.ROOT);
	}

	// Normalize optional text
	private String normalizeOptionalText(String value) {

		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim();
	}
}
