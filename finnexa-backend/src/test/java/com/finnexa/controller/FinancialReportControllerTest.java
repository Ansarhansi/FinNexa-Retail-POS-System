package com.finnexa.controller;

import com.finnexa.dto.DashboardResponse;
import com.finnexa.dto.FinancialReportRequest;
import com.finnexa.dto.FinancialSummaryResponse;
import com.finnexa.service.FinancialReportService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinancialReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class FinancialReportControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private FinancialReportService financialReportService;

	@Test
	void getFinancialSummary_ShouldReturnOk_WhenDatesAreValid() throws Exception {
		LocalDate startDate = LocalDate.of(2026, 1, 1);
		LocalDate endDate = LocalDate.of(2026, 1, 31);

		FinancialSummaryResponse response = new FinancialSummaryResponse(startDate, endDate, new BigDecimal("50000.00"),
				new BigDecimal("10000.00"), new BigDecimal("15000.00"), new BigDecimal("25000.00"));

		when(financialReportService.getFinancialSummary(any(FinancialReportRequest.class))).thenReturn(response);

		mockMvc.perform(get("/api/reports/financial-summary").param("startDate", "2026-01-01")
				.param("endDate", "2026-01-31").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.startDate").value("2026-01-01"))
				.andExpect(jsonPath("$.endDate").value("2026-01-31"))
				.andExpect(jsonPath("$.totalSalesRevenue").value(50000.00))
				.andExpect(jsonPath("$.totalExpenses").value(10000.00))
				.andExpect(jsonPath("$.totalReceivedPurchases").value(15000.00))
				.andExpect(jsonPath("$.estimatedProfitBeforeCostOfGoodsSold").value(25000.00));
	}

	@Test
	void getFinancialSummary_ShouldReturnBadRequest_WhenStartDateIsMissing() throws Exception {
		mockMvc.perform(
				get("/api/reports/financial-summary").param("endDate", "2026-01-31").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getFinancialSummary_ShouldReturnBadRequest_WhenEndDateIsMissing() throws Exception {
		mockMvc.perform(get("/api/reports/financial-summary").param("startDate", "2026-01-01")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	void getFinancialSummary_ShouldReturnBadRequest_WhenBothDatesAreMissing() throws Exception {
		mockMvc.perform(get("/api/reports/financial-summary").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getFinancialSummary_ShouldReturnBadRequest_WhenStartDateFormatIsInvalid() throws Exception {
		mockMvc.perform(get("/api/reports/financial-summary").param("startDate", "01-01-2026")
				.param("endDate", "2026-01-31").accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	void getDashboard_ShouldReturnOk_WhenDashboardDataExists() throws Exception {
		DashboardResponse response = new DashboardResponse();

		when(financialReportService.getDashboard()).thenReturn(response);

		mockMvc.perform(get("/api/reports/dashboard").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
	}

}
