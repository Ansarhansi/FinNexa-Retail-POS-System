package com.finnexa.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finnexa.dto.SaleRequest;
import com.finnexa.dto.SaleResponse;
import com.finnexa.service.SaleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

	private final SaleService saleService;

	public SaleController(SaleService saleService) {
		this.saleService = saleService;
	}

	@PostMapping
	public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody SaleRequest request) {
		SaleResponse response = saleService.createSale(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<SaleResponse>> getAllSales() {
		return ResponseEntity.ok(saleService.getAllSales());
	}

	@GetMapping("/{id}")
	public ResponseEntity<SaleResponse> getSaleById(@PathVariable Long id) {
		return ResponseEntity.ok(saleService.getSaleById(id));
	}

	@GetMapping("/invoice")
	public ResponseEntity<SaleResponse> getSaleByInvoiceNumber(@RequestParam String invoiceNumber) {
		return ResponseEntity.ok(saleService.getSaleByInvoiceNumber(invoiceNumber));
	}

	@GetMapping("/date-range")
	public ResponseEntity<List<SaleResponse>> getSalesBetween(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
		return ResponseEntity.ok(saleService.getSalesBetween(startDate, endDate));
	}

	@PutMapping("/{id}/cancel")
	public ResponseEntity<SaleResponse> cancelSale(@PathVariable Long id) {
		return ResponseEntity.ok(saleService.cancelSale(id));
	}

}