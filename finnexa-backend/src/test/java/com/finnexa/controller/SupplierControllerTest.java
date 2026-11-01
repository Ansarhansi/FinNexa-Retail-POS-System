
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
import com.finnexa.dto.SupplierRequest;
import com.finnexa.dto.SupplierResponse;
import com.finnexa.entity.Supplier;
import com.finnexa.service.SupplierService;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

	@Mock
	private SupplierService supplierService;

	@InjectMocks
	private SupplierController supplierController;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(supplierController).build();

		objectMapper = new ObjectMapper();
	}

	@Test
	void createSupplier_shouldReturnCreatedStatus() throws Exception {
		SupplierRequest request = createRequest();

		when(supplierService.createSupplier(any(SupplierRequest.class))).thenReturn(createResponse());

		mockMvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("ABC Supplies"))
				.andExpect(jsonPath("$.email").value("supplier@example.com"));

		verify(supplierService).createSupplier(any(SupplierRequest.class));
	}

	@Test
	void getAllSuppliers_shouldReturnSupplierList() throws Exception {
		when(supplierService.getAllSuppliers()).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/suppliers")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("ABC Supplies"))
				.andExpect(jsonPath("$[0].email").value("supplier@example.com"));

		verify(supplierService).getAllSuppliers();
	}

	@Test
	void getSupplierById_shouldReturnSupplier() throws Exception {
		when(supplierService.getSupplierById(1L)).thenReturn(createResponse());

		mockMvc.perform(get("/api/suppliers/{id}", 1L)).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("ABC Supplies"))
				.andExpect(jsonPath("$.email").value("supplier@example.com"));

		verify(supplierService).getSupplierById(1L);
	}

	@Test
	void getSupplierByEmail_shouldReturnSupplier() throws Exception {
		when(supplierService.getSupplierByEmail("supplier@example.com")).thenReturn(createResponse());

		mockMvc.perform(get("/api/suppliers/email").param("email", "supplier@example.com")).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("supplier@example.com"));

		verify(supplierService).getSupplierByEmail("supplier@example.com");
	}

	@Test
	void searchSuppliers_shouldReturnMatchingSuppliers() throws Exception {
		when(supplierService.searchSuppliers("ABC")).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/suppliers/search").param("name", "ABC")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("ABC Supplies"));

		verify(supplierService).searchSuppliers("ABC");
	}

	@Test
	void searchByContactPerson_shouldReturnMatchingSuppliers() throws Exception {
		when(supplierService.searchByContactPerson("Ansar")).thenReturn(List.of(createResponse()));

		mockMvc.perform(get("/api/suppliers/search/contact").param("name", "Ansar")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("ABC Supplies"));

		verify(supplierService).searchByContactPerson("Ansar");
	}

	@Test
	void updateSupplier_shouldReturnUpdatedSupplier() throws Exception {
		SupplierRequest request = createRequest();

		when(supplierService.updateSupplier(eq(1L), any(SupplierRequest.class))).thenReturn(createResponse());

		mockMvc.perform(put("/api/suppliers/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("ABC Supplies"))
				.andExpect(jsonPath("$.email").value("supplier@example.com"));

		verify(supplierService).updateSupplier(eq(1L), any(SupplierRequest.class));
	}

	@Test
	void deleteSupplier_shouldReturnNoContent() throws Exception {
		doNothing().when(supplierService).deleteSupplier(1L);

		mockMvc.perform(delete("/api/suppliers/{id}", 1L)).andExpect(status().isNoContent());

		verify(supplierService).deleteSupplier(1L);
	}

	// --------------------------------------------------
	// Helper methods
	// --------------------------------------------------

	private SupplierRequest createRequest() {
		SupplierRequest request = new SupplierRequest();
		request.setName("ABC Supplies");
		request.setEmail("supplier@example.com");
		request.setPhone("9876543210");
		request.setAddress("Bengaluru");
		request.setContactPerson("Ansar");
		return request;
	}

	private SupplierResponse createResponse() {
		Supplier supplier = new Supplier();
		supplier.setId(1L);
		supplier.setName("ABC Supplies");
		supplier.setEmail("supplier@example.com");
		supplier.setPhone("9876543210");
		supplier.setAddress("Bengaluru");
		supplier.setContactPerson("Ansar");

		return new SupplierResponse(supplier);
	}
}
