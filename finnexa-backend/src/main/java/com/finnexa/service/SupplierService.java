
package com.finnexa.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.SupplierRequest;
import com.finnexa.dto.SupplierResponse;
import com.finnexa.entity.Supplier;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.SupplierRepository;

@Service
@Transactional
public class SupplierService {

	private final SupplierRepository supplierRepository;
	private final AuditLogService auditLogService;

	public SupplierService(SupplierRepository supplierRepository, AuditLogService auditLogService) {

		this.supplierRepository = supplierRepository;
		this.auditLogService = auditLogService;
	}

	// Create a supplier
	public SupplierResponse createSupplier(SupplierRequest request) {

		if (request == null) {
			throw new BadRequestException("Supplier request must not be empty.");
		}

		if (request.getName() == null || request.getName().isBlank()) {
			throw new BadRequestException("Supplier name must not be empty.");
		}

		if (request.getContactPerson() == null || request.getContactPerson().isBlank()) {
			throw new BadRequestException("Contact person must not be empty.");
		}

		Supplier supplier = new Supplier();

		supplier.setName(request.getName().trim());
		supplier.setContactPerson(request.getContactPerson().trim());
		supplier.setEmail(normalizeEmail(request.getEmail()));
		supplier.setPhone(normalizeOptionalText(request.getPhone()));
		supplier.setAddress(normalizeOptionalText(request.getAddress()));

		Supplier savedSupplier = supplierRepository.save(supplier);

		auditLogService.recordAction("SUPPLIER_CREATED", "Supplier", savedSupplier.getId(),
				"Supplier created: " + savedSupplier.getName());

		return new SupplierResponse(savedSupplier);
	}

	// Retrieve all suppliers
	@Transactional(readOnly = true)
	public List<SupplierResponse> getAllSuppliers() {

		return supplierRepository.findAll().stream().map(SupplierResponse::new).toList();
	}

	// Retrieve a supplier by ID
	@Transactional(readOnly = true)
	public SupplierResponse getSupplierById(Long id) {

		return new SupplierResponse(findSupplierById(id));
	}

	// Retrieve a supplier by email
	@Transactional(readOnly = true)
	public SupplierResponse getSupplierByEmail(String email) {

		String normalizedEmail = normalizeEmail(email);

		if (normalizedEmail == null) {
			throw new BadRequestException("Email must not be empty.");
		}

		Supplier supplier = supplierRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier not found with email: " + normalizedEmail));

		return new SupplierResponse(supplier);
	}

	// Search suppliers by name
	@Transactional(readOnly = true)
	public List<SupplierResponse> searchSuppliers(String name) {

		if (name == null || name.isBlank()) {
			throw new BadRequestException("Search name must not be empty.");
		}

		return supplierRepository.findByNameContainingIgnoreCase(name.trim()).stream().map(SupplierResponse::new)
				.toList();
	}

	// Search suppliers by contact person
	@Transactional(readOnly = true)
	public List<SupplierResponse> searchByContactPerson(String contactPerson) {

		if (contactPerson == null || contactPerson.isBlank()) {
			throw new BadRequestException("Contact person search must not be empty.");
		}

		return supplierRepository.findByContactPersonContainingIgnoreCase(contactPerson.trim()).stream()
				.map(SupplierResponse::new).toList();
	}

	// Update a supplier
	public SupplierResponse updateSupplier(Long id, SupplierRequest request) {

		if (request == null) {
			throw new BadRequestException("Supplier request must not be empty.");
		}

		if (request.getName() == null || request.getName().isBlank()) {
			throw new BadRequestException("Supplier name must not be empty.");
		}

		if (request.getContactPerson() == null || request.getContactPerson().isBlank()) {
			throw new BadRequestException("Contact person must not be empty.");
		}

		Supplier supplier = findSupplierById(id);

		supplier.setName(request.getName().trim());
		supplier.setContactPerson(request.getContactPerson().trim());
		supplier.setEmail(normalizeEmail(request.getEmail()));
		supplier.setPhone(normalizeOptionalText(request.getPhone()));
		supplier.setAddress(normalizeOptionalText(request.getAddress()));

		Supplier updatedSupplier = supplierRepository.save(supplier);

		auditLogService.recordAction("SUPPLIER_UPDATED", "Supplier", updatedSupplier.getId(),
				"Supplier updated: " + updatedSupplier.getName());

		return new SupplierResponse(updatedSupplier);
	}

	// Delete a supplier
	public void deleteSupplier(Long id) {

		Supplier supplier = findSupplierById(id);

		Long supplierId = supplier.getId();
		String supplierName = supplier.getName();

		supplierRepository.delete(supplier);

		auditLogService.recordAction("SUPPLIER_DELETED", "Supplier", supplierId, "Supplier deleted: " + supplierName);
	}

	// Find supplier by ID
	private Supplier findSupplierById(Long id) {

		if (id == null || id <= 0) {
			throw new BadRequestException("Supplier ID must be a positive number.");
		}

		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + id));
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
