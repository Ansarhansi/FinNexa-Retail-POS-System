
package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.SupplierRequest;
import com.finnexa.dto.SupplierResponse;
import com.finnexa.entity.Supplier;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

	@Mock
	private SupplierRepository supplierRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private SupplierService supplierService;

	private Supplier supplier;
	private SupplierRequest supplierRequest;

	@BeforeEach
	void setUp() {

		supplier = new Supplier();
		supplier.setId(1L);
		supplier.setName("TechWorld Distributors");
		supplier.setContactPerson("Arun Kumar");
		supplier.setEmail("arun@techworld.com");
		supplier.setPhone("9876543210");
		supplier.setAddress("Bengaluru, Karnataka");

		supplierRequest = new SupplierRequest();
		supplierRequest.setName("TechWorld Distributors");
		supplierRequest.setContactPerson("Arun Kumar");
		supplierRequest.setEmail("arun@techworld.com");
		supplierRequest.setPhone("9876543210");
		supplierRequest.setAddress("Bengaluru, Karnataka");
	}

	// 1. Create supplier successfully
	@Test
	void createSupplierShouldReturnCreatedSupplier() {

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
			Supplier saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		SupplierResponse response = supplierService.createSupplier(supplierRequest);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("TechWorld Distributors", response.getName());
		assertEquals("Arun Kumar", response.getContactPerson());
		assertEquals("arun@techworld.com", response.getEmail());
		assertEquals("9876543210", response.getPhone());
		assertEquals("Bengaluru, Karnataka", response.getAddress());

		verify(supplierRepository).save(any(Supplier.class));

		verify(auditLogService).recordAction(eq("SUPPLIER_CREATED"), eq("Supplier"), eq(1L),
				eq("Supplier created: TechWorld Distributors"));
	}

	// 2. Trim supplier name and contact person
	@Test
	void createSupplierShouldTrimNameAndContactPerson() {

		supplierRequest.setName("  TechWorld Distributors  ");
		supplierRequest.setContactPerson("  Arun Kumar  ");

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
			Supplier saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		SupplierResponse response = supplierService.createSupplier(supplierRequest);

		assertEquals("TechWorld Distributors", response.getName());
		assertEquals("Arun Kumar", response.getContactPerson());
	}

	// 3. Normalize email
	@Test
	void createSupplierShouldNormalizeEmail() {

		supplierRequest.setEmail("  ARUN@TECHWORLD.COM  ");

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
			Supplier saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		SupplierResponse response = supplierService.createSupplier(supplierRequest);

		assertEquals("arun@techworld.com", response.getEmail());
	}

	// 4. Reject null request
	@Test
	void createSupplierShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> supplierService.createSupplier(null));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 5. Reject empty supplier name
	@Test
	void createSupplierShouldRejectEmptyName() {

		supplierRequest.setName("  ");

		assertThrows(BadRequestException.class, () -> supplierService.createSupplier(supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 6. Reject null supplier name
	@Test
	void createSupplierShouldRejectNullName() {

		supplierRequest.setName(null);

		assertThrows(BadRequestException.class, () -> supplierService.createSupplier(supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 7. Reject empty contact person
	@Test
	void createSupplierShouldRejectEmptyContactPerson() {

		supplierRequest.setContactPerson("  ");

		assertThrows(BadRequestException.class, () -> supplierService.createSupplier(supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 8. Reject null contact person
	@Test
	void createSupplierShouldRejectNullContactPerson() {

		supplierRequest.setContactPerson(null);

		assertThrows(BadRequestException.class, () -> supplierService.createSupplier(supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 9. Allow missing email
	@Test
	void createSupplierShouldAllowMissingEmail() {

		supplierRequest.setEmail(null);

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
			Supplier saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		SupplierResponse response = supplierService.createSupplier(supplierRequest);

		assertNull(response.getEmail());
	}

	// 10. Convert blank optional fields to null
	@Test
	void createSupplierShouldConvertBlankOptionalFieldsToNull() {

		supplierRequest.setEmail(" ");
		supplierRequest.setPhone(" ");
		supplierRequest.setAddress(" ");

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
			Supplier saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		SupplierResponse response = supplierService.createSupplier(supplierRequest);

		assertNull(response.getEmail());
		assertNull(response.getPhone());
		assertNull(response.getAddress());
	}

	// 11. Retrieve all suppliers
	@Test
	void getAllSuppliersShouldReturnAllSuppliers() {

		when(supplierRepository.findAll()).thenReturn(List.of(supplier));

		List<SupplierResponse> responses = supplierService.getAllSuppliers();

		assertNotNull(responses);
		assertEquals(1, responses.size());
		assertEquals("TechWorld Distributors", responses.get(0).getName());

		verify(supplierRepository).findAll();
	}

	// 12. Return empty list when no suppliers exist
	@Test
	void getAllSuppliersShouldReturnEmptyList() {

		when(supplierRepository.findAll()).thenReturn(List.of());

		List<SupplierResponse> responses = supplierService.getAllSuppliers();

		assertNotNull(responses);
		assertEquals(0, responses.size());
	}

	// 13. Retrieve supplier by ID
	@Test
	void getSupplierByIdShouldReturnSupplier() {

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		SupplierResponse response = supplierService.getSupplierById(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("TechWorld Distributors", response.getName());
	}

	// 14. Handle missing supplier ID
	@Test
	void getSupplierByIdShouldThrowWhenSupplierDoesNotExist() {

		when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> supplierService.getSupplierById(999L));
	}

	// 15. Reject invalid supplier IDs
	@Test
	void getSupplierByIdShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> supplierService.getSupplierById(0L));

		assertThrows(BadRequestException.class, () -> supplierService.getSupplierById(-1L));

		assertThrows(BadRequestException.class, () -> supplierService.getSupplierById(null));
	}

	// 16. Retrieve supplier by email
	@Test
	void getSupplierByEmailShouldReturnSupplier() {

		when(supplierRepository.findByEmail("arun@techworld.com")).thenReturn(Optional.of(supplier));

		SupplierResponse response = supplierService.getSupplierByEmail("  ARUN@TECHWORLD.COM  ");

		assertNotNull(response);
		assertEquals("TechWorld Distributors", response.getName());
		assertEquals("arun@techworld.com", response.getEmail());

		verify(supplierRepository).findByEmail("arun@techworld.com");
	}

	// 17. Handle missing supplier email
	@Test
	void getSupplierByEmailShouldThrowWhenSupplierDoesNotExist() {

		when(supplierRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> supplierService.getSupplierByEmail("missing@example.com"));
	}

	// 18. Reject empty email lookup
	@Test
	void getSupplierByEmailShouldRejectEmptyEmail() {

		assertThrows(BadRequestException.class, () -> supplierService.getSupplierByEmail(" "));
	}

	// 19. Search suppliers by name
	@Test
	void searchSuppliersShouldReturnMatchingSuppliers() {

		when(supplierRepository.findByNameContainingIgnoreCase("TechWorld")).thenReturn(List.of(supplier));

		List<SupplierResponse> responses = supplierService.searchSuppliers(" TechWorld ");

		assertEquals(1, responses.size());
		assertEquals("TechWorld Distributors", responses.get(0).getName());

		verify(supplierRepository).findByNameContainingIgnoreCase("TechWorld");
	}

	// 20. Reject empty supplier name search
	@Test
	void searchSuppliersShouldRejectEmptyName() {

		assertThrows(BadRequestException.class, () -> supplierService.searchSuppliers(" "));
	}

	// 21. Reject null supplier name search
	@Test
	void searchSuppliersShouldRejectNullName() {

		assertThrows(BadRequestException.class, () -> supplierService.searchSuppliers(null));
	}

	// 22. Search by contact person
	@Test
	void searchByContactPersonShouldReturnMatchingSuppliers() {

		when(supplierRepository.findByContactPersonContainingIgnoreCase("Arun")).thenReturn(List.of(supplier));

		List<SupplierResponse> responses = supplierService.searchByContactPerson(" Arun ");

		assertEquals(1, responses.size());
		assertEquals("Arun Kumar", responses.get(0).getContactPerson());

		verify(supplierRepository).findByContactPersonContainingIgnoreCase("Arun");
	}

	// 23. Reject empty contact person search
	@Test
	void searchByContactPersonShouldRejectEmptySearch() {

		assertThrows(BadRequestException.class, () -> supplierService.searchByContactPerson(" "));
	}

	// 24. Reject null contact person search
	@Test
	void searchByContactPersonShouldRejectNullSearch() {

		assertThrows(BadRequestException.class, () -> supplierService.searchByContactPerson(null));
	}

	// 25. Update supplier successfully
	@Test
	void updateSupplierShouldUpdateExistingSupplier() {

		supplierRequest.setName("Updated TechWorld");
		supplierRequest.setContactPerson("Ravi Kumar");
		supplierRequest.setEmail("ravi@techworld.com");
		supplierRequest.setPhone("9988776655");
		supplierRequest.setAddress("Mysuru, Karnataka");

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SupplierResponse response = supplierService.updateSupplier(1L, supplierRequest);

		assertEquals("Updated TechWorld", response.getName());
		assertEquals("Ravi Kumar", response.getContactPerson());
		assertEquals("ravi@techworld.com", response.getEmail());
		assertEquals("9988776655", response.getPhone());
		assertEquals("Mysuru, Karnataka", response.getAddress());

		verify(supplierRepository).save(any(Supplier.class));

		verify(auditLogService).recordAction(eq("SUPPLIER_UPDATED"), eq("Supplier"), eq(1L),
				eq("Supplier updated: Updated TechWorld"));
	}

	// 26. Reject null update request
	@Test
	void updateSupplierShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> supplierService.updateSupplier(1L, null));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 27. Reject empty name during update
	@Test
	void updateSupplierShouldRejectEmptyName() {

		supplierRequest.setName(" ");

		assertThrows(BadRequestException.class, () -> supplierService.updateSupplier(1L, supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 28. Reject empty contact person during update
	@Test
	void updateSupplierShouldRejectEmptyContactPerson() {

		supplierRequest.setContactPerson(" ");

		assertThrows(BadRequestException.class, () -> supplierService.updateSupplier(1L, supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 29. Handle missing supplier during update
	@Test
	void updateSupplierShouldThrowWhenSupplierDoesNotExist() {

		when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> supplierService.updateSupplier(999L, supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 30. Reject invalid supplier ID during update
	@Test
	void updateSupplierShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> supplierService.updateSupplier(0L, supplierRequest));

		verify(supplierRepository, never()).save(any(Supplier.class));
	}

	// 31. Delete supplier and record audit
	@Test
	void deleteSupplierShouldDeleteSupplierAndRecordAudit() {

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		supplierService.deleteSupplier(1L);

		verify(supplierRepository).delete(supplier);

		verify(auditLogService).recordAction(eq("SUPPLIER_DELETED"), eq("Supplier"), eq(1L),
				eq("Supplier deleted: TechWorld Distributors"));
	}

	// 32. Handle missing supplier during deletion
	@Test
	void deleteSupplierShouldThrowWhenSupplierDoesNotExist() {

		when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> supplierService.deleteSupplier(999L));

		verify(supplierRepository, never()).delete(any(Supplier.class));
	}

	// 33. Reject invalid supplier ID during deletion
	@Test
	void deleteSupplierShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> supplierService.deleteSupplier(-1L));

		verify(supplierRepository, never()).delete(any(Supplier.class));
	}

	// 34. Convert blank email to null during update
	@Test
	void updateSupplierShouldConvertBlankEmailToNull() {

		supplierRequest.setEmail(" ");

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SupplierResponse response = supplierService.updateSupplier(1L, supplierRequest);

		assertNull(response.getEmail());
	}

	// 35. Convert blank optional fields to null during update
	@Test
	void updateSupplierShouldConvertBlankOptionalFieldsToNull() {

		supplierRequest.setPhone(" ");
		supplierRequest.setAddress(" ");

		when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

		when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SupplierResponse response = supplierService.updateSupplier(1L, supplierRequest);

		assertNull(response.getPhone());
		assertNull(response.getAddress());
	}
}
