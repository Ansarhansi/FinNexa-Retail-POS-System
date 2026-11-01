package com.finnexa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finnexa.dto.DashboardResponse;
import com.finnexa.dto.FinancialReportRequest;
import com.finnexa.dto.FinancialSummaryResponse;
import com.finnexa.service.FinancialReportService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reports")
public class FinancialReportController {
	private final FinancialReportService financialReportService;

	public FinancialReportController(FinancialReportService financialReportService) {
		this.financialReportService = financialReportService;
	}

	@GetMapping("/financial-summary")
	public ResponseEntity<FinancialSummaryResponse> getFinancialSummary(
			@Valid @ModelAttribute FinancialReportRequest request) {
		FinancialSummaryResponse response = financialReportService.getFinancialSummary(request);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/dashboard")
	public ResponseEntity<DashboardResponse> getDashboard() {
		DashboardResponse response = financialReportService.getDashboard();
		return ResponseEntity.ok(response);
	}
}