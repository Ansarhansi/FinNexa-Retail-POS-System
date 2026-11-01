
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
import com.finnexa.dto.SaleRequest;
import com.finnexa.dto.SaleResponse;
import com.finnexa.entity.Sale;
import com.finnexa.service.SaleService;

@WebMvcTest(SaleController.class)
@AutoConfigureMockMvc(addFilters = false)
class SaleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private SaleService saleService;

	private SaleRequest saleRequest;
	private SaleResponse saleResponse;

	@BeforeEach
	void setUp() {
		SaleRequest.SaleItemRequest itemRequest = new SaleRequest.SaleItemRequest();

		itemRequest.setProductId(1L);
		itemRequest.setQuantity(2);

		saleRequest = new SaleRequest();
		saleRequest.setPaymentMethod(Sale.PaymentMethod.CASH);
		saleRequest.setItems(List.of(itemRequest));

		saleResponse = new SaleResponse() {
			@Override
			public Long getId() {
				return 1L;
			}

			@Override
			public String getInvoiceNumber() {
				return "FNX-TEST-001";
			}

			@Override
			public BigDecimal getTotalAmount() {
				return new BigDecimal("1000.00");
			}

			@Override
			public Sale.PaymentMethod getPaymentMethod() {
				return Sale.PaymentMethod.CASH;
			}

			@Override
			public Sale.SaleStatus getStatus() {
				return Sale.SaleStatus.COMPLETED;
			}

			@Override
			public LocalDateTime getSaleDate() {
				return LocalDateTime.of(2026, 1, 15, 10, 30);
			}

			@Override
			public List<SaleResponse.SaleItemResponse> getItems() {
				return List.of();
			}
		};
	}

	@Test
	void createSale_ShouldReturnCreated() throws Exception {
		when(saleService.createSale(any(SaleRequest.class))).thenReturn(saleResponse);

		mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(saleRequest))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.invoiceNumber").value("FNX-TEST-001"))
				.andExpect(jsonPath("$.totalAmount").value(1000.00));

		verify(saleService).createSale(any(SaleRequest.class));
	}

	@Test
	void getAllSales_ShouldReturnOk() throws Exception {
		when(saleService.getAllSales()).thenReturn(List.of(saleResponse));

		mockMvc.perform(get("/api/sales")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].invoiceNumber").value("FNX-TEST-001"));

		verify(saleService).getAllSales();
	}

	@Test
	void getSaleById_ShouldReturnOk() throws Exception {
		when(saleService.getSaleById(1L)).thenReturn(saleResponse);

		mockMvc.perform(get("/api/sales/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.invoiceNumber").value("FNX-TEST-001"));

		verify(saleService).getSaleById(1L);
	}

	@Test
	void getSaleByInvoiceNumber_ShouldReturnOk() throws Exception {
		when(saleService.getSaleByInvoiceNumber("FNX-TEST-001")).thenReturn(saleResponse);

		mockMvc.perform(get("/api/sales/invoice").param("invoiceNumber", "FNX-TEST-001")).andExpect(status().isOk())
				.andExpect(jsonPath("$.invoiceNumber").value("FNX-TEST-001"));

		verify(saleService).getSaleByInvoiceNumber("FNX-TEST-001");
	}

	@Test
	void getSalesBetween_ShouldReturnOk() throws Exception {
		LocalDateTime startDate = LocalDateTime.of(2026, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2026, 1, 31, 23, 59);

		when(saleService.getSalesBetween(startDate, endDate)).thenReturn(List.of(saleResponse));

		mockMvc.perform(get("/api/sales/date-range").param("startDate", "2026-01-01T00:00:00").param("endDate",
				"2026-01-31T23:59:00")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].invoiceNumber").value("FNX-TEST-001"));

		verify(saleService).getSalesBetween(startDate, endDate);
	}
}
