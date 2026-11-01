
package com.finnexa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finnexa.dto.CustomerRequest;
import com.finnexa.dto.CustomerResponse;
import com.finnexa.entity.Customer;
import com.finnexa.service.CustomerService;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

	@Mock
	private CustomerService customerService;

	@InjectMocks
	private CustomerController customerController;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(customerController).build();

		objectMapper = new ObjectMapper();
	}

	@Test
	void createCustomer_shouldReturnCreatedStatus() throws Exception {
		CustomerRequest request = createRequest();
		CustomerResponse response = createResponse();

		when(customerService.createCustomer(any(CustomerRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Ansar")).andExpect(jsonPath("$.email").value("ansar@example.com"));

		verify(customerService).createCustomer(any(CustomerRequest.class));
	}

	@Test
	void getAllCustomers_shouldReturnCustomerList() throws Exception {
		when(customerService.getAllCustomers()).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/customers")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Ansar"))
				.andExpect(jsonPath("$[0].email").value("ansar@example.com"));

		verify(customerService).getAllCustomers();
	}

	@Test
	void getCustomerById_shouldReturnCustomer() throws Exception {
		when(customerService.getCustomerById(1L)).thenReturn(createResponse());

		mockMvc.perform(get("/api/customers/{id}", 1L)).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ansar")).andExpect(jsonPath("$.email").value("ansar@example.com"));

		verify(customerService).getCustomerById(1L);
	}

	@Test
	void getCustomerByEmail_shouldReturnCustomer() throws Exception {
		when(customerService.getCustomerByEmail("ansar@example.com")).thenReturn(createResponse());

		mockMvc.perform(get("/api/customers/email").param("email", "ansar@example.com")).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ansar@example.com"));

		verify(customerService).getCustomerByEmail("ansar@example.com");
	}

	@Test
	void searchCustomers_shouldReturnMatchingCustomers() throws Exception {
		when(customerService.searchCustomers("Ansar")).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/customers/search").param("name", "Ansar")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Ansar"));

		verify(customerService).searchCustomers("Ansar");
	}

	@Test
	void updateCustomer_shouldReturnUpdatedCustomer() throws Exception {
		CustomerRequest request = createRequest();

		when(customerService.updateCustomer(eq(1L), any(CustomerRequest.class))).thenReturn(createResponse());

		mockMvc.perform(put("/api/customers/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ansar")).andExpect(jsonPath("$.email").value("ansar@example.com"));

		verify(customerService).updateCustomer(eq(1L), any(CustomerRequest.class));
	}

	@Test
	void deleteCustomer_shouldReturnNoContent() throws Exception {
		doNothing().when(customerService).deleteCustomer(1L);

		mockMvc.perform(delete("/api/customers/{id}", 1L)).andExpect(status().isNoContent());

		verify(customerService).deleteCustomer(1L);
	}

	// ---------------------------------------------
	// Helper methods
	// ---------------------------------------------

	private CustomerRequest createRequest() {
		CustomerRequest request = new CustomerRequest();
		request.setName("Ansar");
		request.setEmail("ansar@example.com");
		request.setPhone("9876543210");
		request.setAddress("Bengaluru");
		return request;
	}

	private CustomerResponse createResponse() {
		Customer customer = new Customer();
		customer.setId(1L);
		customer.setName("Ansar");
		customer.setEmail("ansar@example.com");
		customer.setPhone("9876543210");
		customer.setAddress("Bengaluru");

		return new CustomerResponse(customer);
	}
}
