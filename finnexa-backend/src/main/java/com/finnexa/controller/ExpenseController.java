package com.finnexa.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finnexa.dto.ExpenseRequest;
import com.finnexa.dto.ExpenseResponse;
import com.finnexa.entity.Expense;
import com.finnexa.service.ExpenseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

	private final ExpenseService expenseService;

	public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}

// Create an expense
	@PostMapping
	public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request) {

		ExpenseResponse response = expenseService.createExpense(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

// Retrieve all expenses
	@GetMapping
	public ResponseEntity<List<ExpenseResponse>> getAllExpenses() {
		return ResponseEntity.ok(expenseService.getAllExpenses());
	}

// Retrieve an expense by ID
	@GetMapping("/{id}")
	public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {

		return ResponseEntity.ok(expenseService.getExpenseById(id));
	}

// Filter expenses by category
	@GetMapping("/category")
	public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(@RequestParam Expense.ExpenseCategory category) {

		return ResponseEntity.ok(expenseService.getExpensesByCategory(category));
	}

// Filter expenses by date range
	@GetMapping("/date-range")
	public ResponseEntity<List<ExpenseResponse>> getExpensesBetween(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		return ResponseEntity.ok(expenseService.getExpensesBetween(startDate, endDate));
	}

// Search expenses by title
	@GetMapping("/search")
	public ResponseEntity<List<ExpenseResponse>> searchExpenses(@RequestParam String title) {

		return ResponseEntity.ok(expenseService.searchExpenses(title));
	}

// Update an expense
	@PutMapping("/{id}")
	public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
			@Valid @RequestBody ExpenseRequest request) {

		return ResponseEntity.ok(expenseService.updateExpense(id, request));
	}

// Delete an expense
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {

		expenseService.deleteExpense(id);

		return ResponseEntity.noContent().build();
	}

}