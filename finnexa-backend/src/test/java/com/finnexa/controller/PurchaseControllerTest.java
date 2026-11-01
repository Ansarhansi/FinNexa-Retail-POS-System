
package com.finnexa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finnexa.dto.PurchaseRequest;
import com.finnexa.dto.PurchaseResponse;
import com.finnexa.entity.Purchase;
import com.finnexa.service.PurchaseService;

@WebMvcTest(PurchaseController.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PurchaseService purchaseService;

	private PurchaseRequest purchaseRequest;
	private PurchaseResponse purchaseResponse;

	@BeforeEach
	void setUp() {
		PurchaseRequest.PurchaseItemRequest itemRequest = new PurchaseRequest.PurchaseItemRequest();

		itemRequest.setProductId(1L);
		itemRequest.setQuantity(2);
		itemRequest.setUnitCost(new BigDecimal("100.00"));

		purchaseRequest = new PurchaseRequest();
		purchaseRequest.setSupplierId(1L);
		purchaseRequest.setItems(List.of(itemRequest));

		purchaseResponse = new PurchaseResponse() {
			@Override
			public Long getId() {
				return 1L;
			}

			@Override
			public String getPurchaseNumber() {
				return "PUR-TEST-001";
			}

			@Override
			public Long getSupplierId() {
				return 1L;
			}

			@Override
			public String getSupplierName() {
				return "Test Supplier";
			}

			@Override
			public BigDecimal getTotalAmount() {
				return new BigDecimal("200.00");
			}

			@Override
			public Purchase.PurchaseStatus getStatus() {
				return Purchase.PurchaseStatus.PENDING;
			}

			@Override
			public LocalDateTime getPurchaseDate() {
				return LocalDateTime.of(2026, 1, 15, 10, 30);
			}

			@Override
			public List<PurchaseResponse.PurchaseItemResponse> getItems() {
				return List.of();
			}
		};
	}

	@Test
	void createPurchase_ShouldReturnCreated() throws Exception {
		when(purchaseService.createPurchase(any(PurchaseRequest.class))).thenReturn(purchaseResponse);

		mockMvc.perform(post("/api/purchases").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(purchaseRequest))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.purchaseNumber").value("PUR-TEST-001"))
				.andExpect(jsonPath("$.supplierId").value(1)).andExpect(jsonPath("$.totalAmount").value(200.00));

		verify(purchaseService).createPurchase(any(PurchaseRequest.class));
	}

	@Test
	void getAllPurchases_ShouldReturnOk() throws Exception {
		when(purchaseService.getAllPurchases()).thenReturn(List.of(purchaseResponse));

		mockMvc.perform(get("/api/purchases")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].purchaseNumber").value("PUR-TEST-001"));

		verify(purchaseService).getAllPurchases();
	}

	@Test
	void getPurchaseById_ShouldReturnOk() throws Exception {
		when(purchaseService.getPurchaseById(1L)).thenReturn(purchaseResponse);

		mockMvc.perform(get("/api/purchases/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.purchaseNumber").value("PUR-TEST-001"));

		verify(purchaseService).getPurchaseById(1L);
	}

	@Test
	void getPurchaseByNumber_ShouldReturnOk() throws Exception {
		when(purchaseService.getPurchaseByNumber("PUR-TEST-001")).thenReturn(purchaseResponse);

		mockMvc.perform(get("/api/purchases/number").param("purchaseNumber", "PUR-TEST-001")).andExpect(status().isOk())
				.andExpect(jsonPath("$.purchaseNumber").value("PUR-TEST-001"));

		verify(purchaseService).getPurchaseByNumber("PUR-TEST-001");
	}

	@Test
	void getPurchasesBySupplier_ShouldReturnOk() throws Exception {
		when(purchaseService.getPurchasesBySupplier(1L)).thenReturn(List.of(purchaseResponse));

		mockMvc.perform(get("/api/purchases/supplier/1")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].supplierId").value(1))
				.andExpect(jsonPath("$[0].supplierName").value("Test Supplier"));

		verify(purchaseService).getPurchasesBySupplier(1L);
	}

	@Test
	void receivePurchase_ShouldReturnOk() throws Exception {
		when(purchaseService.receivePurchase(1L)).thenReturn(purchaseResponse);

		mockMvc.perform(post("/api/purchases/1/receive")).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.purchaseNumber").value("PUR-TEST-001"));

		verify(purchaseService).receivePurchase(1L);
	}

	@Test
	void cancelPurchase_ShouldReturnOk() throws Exception {
		when(purchaseService.cancelPurchase(1L)).thenReturn(purchaseResponse);

		mockMvc.perform(post("/api/purchases/1/cancel")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.purchaseNumber").value("PUR-TEST-001"));

		verify(purchaseService).cancelPurchase(1L);
	}
}
