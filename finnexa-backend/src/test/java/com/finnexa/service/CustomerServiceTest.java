
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

import com.finnexa.dto.CustomerRequest;
import com.finnexa.dto.CustomerResponse;
import com.finnexa.entity.Customer;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private CustomerService customerService;

	private Customer customer;
	private CustomerRequest customerRequest;

	@BeforeEach
	void setUp() {

		customer = new Customer();
		customer.setId(1L);
		customer.setName("Rahul Sharma");
		customer.setEmail("rahul@example.com");
		customer.setPhone("9876543210");
		customer.setAddress("Bengaluru, Karnataka");

		customerRequest = new CustomerRequest();
		customerRequest.setName("Rahul Sharma");
		customerRequest.setEmail("rahul@example.com");
		customerRequest.setPhone("9876543210");
		customerRequest.setAddress("Bengaluru, Karnataka");
	}

	// 1. Create a customer successfully
	@Test
	void createCustomerShouldReturnCreatedCustomer() {

		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		CustomerResponse response = customerService.createCustomer(customerRequest);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Rahul Sharma", response.getName());
		assertEquals("rahul@example.com", response.getEmail());
		assertEquals("9876543210", response.getPhone());
		assertEquals("Bengaluru, Karnataka", response.getAddress());

		verify(customerRepository).save(any(Customer.class));

		verify(auditLogService).recordAction(eq("CUSTOMER_CREATED"), eq("Customer"), eq(1L),
				eq("Customer created: Rahul Sharma"));
	}

	// 2. Trim customer name and optional text
	@Test
	void createCustomerShouldNormalizeCustomerFields() {

		customerRequest.setName("  Rahul Sharma  ");
		customerRequest.setPhone("  9876543210  ");
		customerRequest.setAddress("  Bengaluru  ");

		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		CustomerResponse response = customerService.createCustomer(customerRequest);

		assertEquals("Rahul Sharma", response.getName());
		assertEquals("9876543210", response.getPhone());
		assertEquals("Bengaluru", response.getAddress());
	}

	// 3. Normalize email to lowercase
	@Test
	void createCustomerShouldNormalizeEmail() {

		customerRequest.setEmail("  RAHUL@EXAMPLE.COM  ");

		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		CustomerResponse response = customerService.createCustomer(customerRequest);

		assertEquals("rahul@example.com", response.getEmail());
	}

	// 4. Reject null creation request
	@Test
	void createCustomerShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> customerService.createCustomer(null));

		verify(customerRepository, never()).save(any(Customer.class));
	}

	// 5. Reject empty customer name
	@Test
	void createCustomerShouldRejectEmptyName() {

		customerRequest.setName("   ");

		assertThrows(BadRequestException.class, () -> customerService.createCustomer(customerRequest));

		verify(customerRepository, never()).save(any(Customer.class));
	}

	// 6. Reject null customer name
	@Test
	void createCustomerShouldRejectNullName() {

		customerRequest.setName(null);

		assertThrows(BadRequestException.class, () -> customerService.createCustomer(customerRequest));

		verify(customerRepository, never()).save(any(Customer.class));
	}

	// 7. Allow a missing email
	@Test
	void createCustomerShouldAllowMissingEmail() {

		customerRequest.setEmail(null);

		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		CustomerResponse response = customerService.createCustomer(customerRequest);

		assertNull(response.getEmail());
	}

	// 8. Convert blank optional fields to null
	@Test
	void createCustomerShouldConvertBlankOptionalFieldsToNull() {

		customerRequest.setEmail("  ");
		customerRequest.setPhone("  ");
		customerRequest.setAddress("  ");

		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer saved = invocation.getArgument(0);
			saved.setId(1L);
			return saved;
		});

		CustomerResponse response = customerService.createCustomer(customerRequest);

		assertNull(response.getEmail());
		assertNull(response.getPhone());
		assertNull(response.getAddress());
	}

	// 9. Retrieve all customers
	@Test
	void getAllCustomersShouldReturnAllCustomers() {

		when(customerRepository.findAll()).thenReturn(List.of(customer));

		List<CustomerResponse> responses = customerService.getAllCustomers();

		assertNotNull(responses);
		assertEquals(1, responses.size());
		assertEquals("Rahul Sharma", responses.get(0).getName());

		verify(customerRepository).findAll();
	}

	// 10. Return an empty list when no customers exist
	@Test
	void getAllCustomersShouldReturnEmptyListWhenNoCustomersExist() {

		when(customerRepository.findAll()).thenReturn(List.of());

		List<CustomerResponse> responses = customerService.getAllCustomers();

		assertNotNull(responses);
		assertEquals(0, responses.size());
	}

	// 11. Retrieve customer by ID
	@Test
	void getCustomerByIdShouldReturnCustomer() {

		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		CustomerResponse response = customerService.getCustomerById(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Rahul Sharma", response.getName());
	}

	// 12. Handle missing customer ID
	@Test
	void getCustomerByIdShouldThrowWhenCustomerDoesNotExist() {

		when(customerRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerById(999L));
	}

	// 13. Reject invalid customer IDs
	@Test
	void getCustomerByIdShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> customerService.getCustomerById(0L));

		assertThrows(BadRequestException.class, () -> customerService.getCustomerById(-1L));

		assertThrows(BadRequestException.class, () -> customerService.getCustomerById(null));
	}

	// 14. Retrieve customer by email
	@Test
	void getCustomerByEmailShouldReturnCustomer() {

		when(customerRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(customer));

		CustomerResponse response = customerService.getCustomerByEmail("  RAHUL@EXAMPLE.COM  ");

		assertNotNull(response);
		assertEquals("Rahul Sharma", response.getName());
		assertEquals("rahul@example.com", response.getEmail());

		verify(customerRepository).findByEmail("rahul@example.com");
	}

	// 15. Handle missing customer email
	@Test
	void getCustomerByEmailShouldThrowWhenCustomerDoesNotExist() {

		when(customerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> customerService.getCustomerByEmail("missing@example.com"));
	}

	// 16. Reject empty email lookup
	@Test
	void getCustomerByEmailShouldRejectEmptyEmail() {

		assertThrows(BadRequestException.class, () -> customerService.getCustomerByEmail("  "));
	}

	// 17. Reject null email lookup
	@Test
	void getCustomerByEmailShouldRejectNullEmail() {

		assertThrows(BadRequestException.class, () -> customerService.getCustomerByEmail(null));
	}

	// 18. Search customers by name
	@Test
	void searchCustomersShouldReturnMatchingCustomers() {

		when(customerRepository.findByNameContainingIgnoreCase("Rahul")).thenReturn(List.of(customer));

		List<CustomerResponse> responses = customerService.searchCustomers(" Rahul ");

		assertEquals(1, responses.size());
		assertEquals("Rahul Sharma", responses.get(0).getName());

		verify(customerRepository).findByNameContainingIgnoreCase("Rahul");
	}

	// 19. Reject empty customer search
	@Test
	void searchCustomersShouldRejectEmptyName() {

		assertThrows(BadRequestException.class, () -> customerService.searchCustomers("  "));
	}

	// 20. Reject null customer search
	@Test
	void searchCustomersShouldRejectNullName() {

		assertThrows(BadRequestException.class, () -> customerService.searchCustomers(null));
	}

	// 21. Update customer successfully
	@Test
	void updateCustomerShouldUpdateExistingCustomer() {

		customerRequest.setName("Rahul Kumar");
		customerRequest.setEmail("rahulkumar@example.com");
		customerRequest.setPhone("9988776655");
		customerRequest.setAddress("Mysuru, Karnataka");

		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerResponse response = customerService.updateCustomer(1L, customerRequest);

		assertEquals("Rahul Kumar", response.getName());
		assertEquals("rahulkumar@example.com", response.getEmail());
		assertEquals("9988776655", response.getPhone());
		assertEquals("Mysuru, Karnataka", response.getAddress());

		verify(customerRepository).save(any(Customer.class));

		verify(auditLogService).recordAction(eq("CUSTOMER_UPDATED"), eq("Customer"), eq(1L),
				eq("Customer updated: Rahul Kumar"));
	}

	// 22. Reject null update request
	@Test
	void updateCustomerShouldRejectNullRequest() {

		assertThrows(BadRequestException.class, () -> customerService.updateCustomer(1L, null));

		verify(customerRepository, never()).save(any(Customer.class));
	}

	// 23. Reject empty name during update
	@Test
	void updateCustomerShouldRejectEmptyName() {

		customerRequest.setName(" ");

		assertThrows(BadRequestException.class, () -> customerService.updateCustomer(1L, customerRequest));

		verify(customerRepository, never()).save(any(Customer.class));
	}

	// 24. Handle missing customer during update
	@Test
	void updateCustomerShouldThrowWhenCustomerDoesNotExist() {

		when(customerRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> customerService.updateCustomer(999L, customerRequest));

		verify(customerRepository, never()).save(any(Customer.class));
	}

	// 25. Reject invalid customer ID during update
	@Test
	void updateCustomerShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> customerService.updateCustomer(0L, customerRequest));

		verify(customerRepository, never()).save(any(Customer.class));
	}

	// 26. Delete customer successfully
	@Test
	void deleteCustomerShouldDeleteCustomerAndRecordAudit() {

		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		customerService.deleteCustomer(1L);

		verify(customerRepository).delete(customer);

		verify(auditLogService).recordAction(eq("CUSTOMER_DELETED"), eq("Customer"), eq(1L),
				eq("Customer deleted: Rahul Sharma"));
	}

	// 27. Handle missing customer during deletion
	@Test
	void deleteCustomerShouldThrowWhenCustomerDoesNotExist() {

		when(customerRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> customerService.deleteCustomer(999L));

		verify(customerRepository, never()).delete(any(Customer.class));
	}

	// 28. Reject invalid customer ID during deletion
	@Test
	void deleteCustomerShouldRejectInvalidId() {

		assertThrows(BadRequestException.class, () -> customerService.deleteCustomer(-1L));

		verify(customerRepository, never()).delete(any(Customer.class));
	}

	// 29. Convert blank email to null when updating
	@Test
	void updateCustomerShouldConvertBlankEmailToNull() {

		customerRequest.setEmail(" ");

		when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerResponse response = customerService.updateCustomer(1L, customerRequest);

		assertNull(response.getEmail());
	}
}
