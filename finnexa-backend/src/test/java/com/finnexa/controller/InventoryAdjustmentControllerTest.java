
package com.finnexa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.finnexa.dto.InventoryAdjustmentRequest;
import com.finnexa.dto.InventoryAdjustmentResponse;
import com.finnexa.service.InventoryAdjustmentService;

@WebMvcTest(InventoryAdjustmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryAdjustmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private InventoryAdjustmentService inventoryAdjustmentService;

	private InventoryAdjustmentRequest adjustmentRequest;
	private InventoryAdjustmentResponse adjustmentResponse;

	@BeforeEach
	void setUp() {
		adjustmentRequest = new InventoryAdjustmentRequest();
		adjustmentRequest.setProductId(1L);
		adjustmentRequest.setQuantityChange(5);
		adjustmentRequest.setReason("Stock correction");

		adjustmentResponse = new InventoryAdjustmentResponse(1L, 1L, "Laptop", 5, 10, 15, "Stock correction",
				LocalDateTime.of(2026, 10, 28, 10, 0));
	}

	@Test
	void adjustStock_ShouldReturnCreated() throws Exception {
		when(inventoryAdjustmentService.adjustStock(any(InventoryAdjustmentRequest.class)))
				.thenReturn(adjustmentResponse);

		mockMvc.perform(post("/api/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(adjustmentRequest))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.productId").value(1))
				.andExpect(jsonPath("$.productName").value("Laptop")).andExpect(jsonPath("$.quantityChange").value(5))
				.andExpect(jsonPath("$.previousQuantity").value(10)).andExpect(jsonPath("$.newQuantity").value(15))
				.andExpect(jsonPath("$.reason").value("Stock correction"));

		verify(inventoryAdjustmentService).adjustStock(any(InventoryAdjustmentRequest.class));
	}

	@Test
	void getAllAdjustments_ShouldReturnOk() throws Exception {
		when(inventoryAdjustmentService.getAllAdjustments()).thenReturn(List.of(adjustmentResponse));

		mockMvc.perform(get("/api/inventory/adjustments")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1)).andExpect(jsonPath("$[0].productId").value(1))
				.andExpect(jsonPath("$[0].productName").value("Laptop"))
				.andExpect(jsonPath("$[0].quantityChange").value(5)).andExpect(jsonPath("$[0].newQuantity").value(15));

		verify(inventoryAdjustmentService).getAllAdjustments();
	}

	@Test
	void getAdjustmentsByProductId_ShouldReturnOk() throws Exception {
		when(inventoryAdjustmentService.getAdjustmentsByProductId(1L)).thenReturn(List.of(adjustmentResponse));

		mockMvc.perform(get("/api/inventory/adjustments/product/1")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(1)).andExpect(jsonPath("$[0].productName").value("Laptop"))
				.andExpect(jsonPath("$[0].reason").value("Stock correction"));

		verify(inventoryAdjustmentService).getAdjustmentsByProductId(1L);
	}
}
