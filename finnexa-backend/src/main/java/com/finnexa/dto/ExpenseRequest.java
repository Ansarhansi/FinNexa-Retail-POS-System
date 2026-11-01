package com.finnexa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.finnexa.entity.Expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ExpenseRequest {

	@NotBlank(message = "Expense title is required")
	@Size(max = 150, message = "Title cannot exceed 150 characters")
	private String title;

	@NotBlank(message = "Expense description is required")
	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	@NotNull(message = "Expense amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be greater than zero")
	private BigDecimal amount;

	@NotNull(message = "Expense category is required")
	private Expense.ExpenseCategory category;

	@NotNull(message = "Payment method is required")
	private Expense.PaymentMethod paymentMethod;

	@NotNull(message = "Expense date is required")
	private LocalDate expenseDate;

	public ExpenseRequest() {
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Expense.ExpenseCategory getCategory() {
		return category;
	}

	public void setCategory(Expense.ExpenseCategory category) {
		this.category = category;
	}

	public Expense.PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(Expense.PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public LocalDate getExpenseDate() {
		return expenseDate;
	}

	public void setExpenseDate(LocalDate expenseDate) {
		this.expenseDate = expenseDate;
	}

}