
package com.finnexa.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.ExpenseRequest;
import com.finnexa.dto.ExpenseResponse;
import com.finnexa.entity.Expense;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.ExpenseRepository;

@Service
@Transactional
public class ExpenseService {

	private final ExpenseRepository expenseRepository;
	private final AuditLogService auditLogService;

	public ExpenseService(ExpenseRepository expenseRepository, AuditLogService auditLogService) {

		this.expenseRepository = expenseRepository;
		this.auditLogService = auditLogService;
	}

	// Create an expense
	public ExpenseResponse createExpense(ExpenseRequest request) {

		Expense expense = new Expense();
		applyRequest(expense, request);

		Expense savedExpense = expenseRepository.save(expense);

		auditLogService.recordAction("EXPENSE_CREATED", "Expense", savedExpense.getId(),
				"Expense created: " + savedExpense.getTitle() + "; Amount: " + savedExpense.getAmount() + "; Category: "
						+ savedExpense.getCategory() + "; Payment method: " + savedExpense.getPaymentMethod());

		return new ExpenseResponse(savedExpense);
	}

	// Retrieve all expenses
	@Transactional(readOnly = true)
	public List<ExpenseResponse> getAllExpenses() {

		return expenseRepository.findAll().stream().map(ExpenseResponse::new).toList();
	}

	// Retrieve an expense by ID
	@Transactional(readOnly = true)
	public ExpenseResponse getExpenseById(Long id) {

		return new ExpenseResponse(findExpenseById(id));
	}

	// Retrieve expenses by category
	@Transactional(readOnly = true)
	public List<ExpenseResponse> getExpensesByCategory(Expense.ExpenseCategory category) {

		if (category == null) {
			throw new BadRequestException("Expense category is required.");
		}

		return expenseRepository.findByCategory(category).stream().map(ExpenseResponse::new).toList();
	}

	// Retrieve expenses between dates
	@Transactional(readOnly = true)
	public List<ExpenseResponse> getExpensesBetween(LocalDate startDate, LocalDate endDate) {

		validateDateRange(startDate, endDate);

		return expenseRepository.findByExpenseDateBetween(startDate, endDate).stream().map(ExpenseResponse::new)
				.toList();
	}

	// Search expenses by title
	@Transactional(readOnly = true)
	public List<ExpenseResponse> searchExpenses(String title) {

		if (title == null || title.isBlank()) {
			throw new BadRequestException("Expense title search must not be empty.");
		}

		return expenseRepository.findByTitleContainingIgnoreCase(title.trim()).stream().map(ExpenseResponse::new)
				.toList();
	}

	// Update an expense
	public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {

		Expense expense = findExpenseById(id);
		applyRequest(expense, request);

		Expense updatedExpense = expenseRepository.save(expense);

		auditLogService.recordAction("EXPENSE_UPDATED", "Expense", updatedExpense.getId(),
				"Expense updated: " + updatedExpense.getTitle() + "; Amount: " + updatedExpense.getAmount()
						+ "; Category: " + updatedExpense.getCategory() + "; Payment method: "
						+ updatedExpense.getPaymentMethod());

		return new ExpenseResponse(updatedExpense);
	}

	// Delete an expense
	public void deleteExpense(Long id) {

		Expense expense = findExpenseById(id);

		Long expenseId = expense.getId();
		String expenseTitle = expense.getTitle();
		String amount = expense.getAmount().toPlainString();

		expenseRepository.delete(expense);

		auditLogService.recordAction("EXPENSE_DELETED", "Expense", expenseId,
				"Expense deleted: " + expenseTitle + "; Amount: " + amount);
	}

	// Find an expense by ID
	private Expense findExpenseById(Long id) {

		if (id == null || id <= 0) {
			throw new BadRequestException("Expense ID must be a positive number.");
		}

		return expenseRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));
	}

	// Validate and apply request fields
	private void applyRequest(Expense expense, ExpenseRequest request) {

		if (request == null) {
			throw new BadRequestException("Expense request is required.");
		}

		if (request.getTitle() == null || request.getTitle().isBlank()) {
			throw new BadRequestException("Expense title is required.");
		}

		if (request.getDescription() == null || request.getDescription().isBlank()) {
			throw new BadRequestException("Expense description is required.");
		}

		if (request.getAmount() == null || request.getAmount().signum() <= 0) {
			throw new BadRequestException("Expense amount must be greater than zero.");
		}

		if (request.getCategory() == null || request.getPaymentMethod() == null) {
			throw new BadRequestException("Expense category and payment method are required.");
		}

		if (request.getExpenseDate() == null) {
			throw new BadRequestException("Expense date is required.");
		}

		expense.setTitle(request.getTitle().trim());
		expense.setDescription(request.getDescription().trim());
		expense.setAmount(request.getAmount());
		expense.setCategory(request.getCategory());
		expense.setPaymentMethod(request.getPaymentMethod());
		expense.setExpenseDate(request.getExpenseDate());
	}

	// Validate date range
	private void validateDateRange(LocalDate startDate, LocalDate endDate) {

		if (startDate == null || endDate == null) {
			throw new BadRequestException("Start date and end date are required.");
		}

		if (startDate.isAfter(endDate)) {
			throw new BadRequestException("Start date must not be after end date.");
		}
	}
}
