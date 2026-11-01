
package com.finnexa.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.finnexa.dto.InventoryResponse;
import com.finnexa.dto.InventorySummaryResponse;
import com.finnexa.service.InventoryService;
import com.finnexa.service.InventorySummaryService;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private InventoryService inventoryService;

	@MockBean
	private InventorySummaryService inventorySummaryService;

	private InventoryResponse inventoryResponse;
	private InventorySummaryResponse summaryResponse;

	@BeforeEach
	void setUp() {
		inventoryResponse = new InventoryResponse(1L, "Laptop", "LAP-001", 15, new BigDecimal("50000.00"), false);

		summaryResponse = new InventorySummaryResponse(100L, 10L, 5L, new BigDecimal("2500000.00"));
	}

	@Test
	void getAllInventory_ShouldReturnOk() throws Exception {
		when(inventoryService.getAllInventory()).thenReturn(List.of(inventoryResponse));

		mockMvc.perform(get("/api/inventory")).andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(1))
				.andExpect(jsonPath("$[0].productName").value("Laptop"))
				.andExpect(jsonPath("$[0].sku").value("LAP-001"))
				.andExpect(jsonPath("$[0].availableQuantity").value(15))
				.andExpect(jsonPath("$[0].unitPrice").value(50000.00))
				.andExpect(jsonPath("$[0].lowStock").value(false));

		verify(inventoryService).getAllInventory();
	}

	@Test
	void getInventoryByProductId_ShouldReturnOk() throws Exception {
		when(inventoryService.getInventoryByProductId(1L)).thenReturn(inventoryResponse);

		mockMvc.perform(get("/api/inventory/1")).andExpect(status().isOk()).andExpect(jsonPath("$.productId").value(1))
				.andExpect(jsonPath("$.productName").value("Laptop")).andExpect(jsonPath("$.sku").value("LAP-001"));

		verify(inventoryService).getInventoryByProductId(1L);
	}

	@Test
	void getLowStockInventory_ShouldReturnOk() throws Exception {
		InventoryResponse lowStockResponse = new InventoryResponse(2L, "Mouse", "MOU-001", 2, new BigDecimal("500.00"),
				true);

		when(inventoryService.getLowStockInventory()).thenReturn(List.of(lowStockResponse));

		mockMvc.perform(get("/api/inventory/low-stock")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].productId").value(2)).andExpect(jsonPath("$[0].productName").value("Mouse"))
				.andExpect(jsonPath("$[0].availableQuantity").value(2))
				.andExpect(jsonPath("$[0].lowStock").value(true));

		verify(inventoryService).getLowStockInventory();
	}

	@Test
	void getInventorySummary_ShouldReturnOk() throws Exception {
		when(inventorySummaryService.getInventorySummary()).thenReturn(summaryResponse);

		mockMvc.perform(get("/api/inventory/summary")).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalProducts").value(100)).andExpect(jsonPath("$.lowStockProducts").value(10))
				.andExpect(jsonPath("$.outOfStockProducts").value(5))
				.andExpect(jsonPath("$.totalInventorySellingValue").value(2500000.00));

		verify(inventorySummaryService).getInventorySummary();
	}

	@Test
	void getTotalInventorySellingValue_ShouldReturnOk() throws Exception {
		BigDecimal totalValue = new BigDecimal("2500000.00");

		when(inventoryService.getTotalInventorySellingValue()).thenReturn(totalValue);

		mockMvc.perform(get("/api/inventory/total-value")).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(2500000.00));

		verify(inventoryService).getTotalInventorySellingValue();
	}

	@Test
	void getTotalProductCount_ShouldReturnOk() throws Exception {
		when(inventoryService.getTotalProductCount()).thenReturn(100L);

		mockMvc.perform(get("/api/inventory/count")).andExpect(status().isOk()).andExpect(jsonPath("$").value(100));

		verify(inventoryService).getTotalProductCount();
	}

	@Test
	void getLowStockProductCount_ShouldReturnOk() throws Exception {
		when(inventoryService.getLowStockProductCount()).thenReturn(10L);

		mockMvc.perform(get("/api/inventory/low-stock/count")).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(10));

		verify(inventoryService).getLowStockProductCount();
	}
}
