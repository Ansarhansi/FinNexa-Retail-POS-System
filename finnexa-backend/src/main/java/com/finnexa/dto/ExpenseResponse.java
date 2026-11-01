package com.finnexa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.finnexa.entity.Expense;

public class ExpenseResponse {

	private Long id;
	private String title;
	private String description;
	private BigDecimal amount;
	private Expense.ExpenseCategory category;
	private Expense.PaymentMethod paymentMethod;
	private LocalDate expenseDate;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public ExpenseResponse() {
	}

	public ExpenseResponse(Expense expense) {
		this.id = expense.getId();
		this.title = expense.getTitle();
		this.description = expense.getDescription();
		this.amount = expense.getAmount();
		this.category = expense.getCategory();
		this.paymentMethod = expense.getPaymentMethod();
		this.expenseDate = expense.getExpenseDate();
		this.createdAt = expense.getCreatedAt();
		this.updatedAt = expense.getUpdatedAt();
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public Expense.ExpenseCategory getCategory() {
		return category;
	}

	public Expense.PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public LocalDate getExpenseDate() {
		return expenseDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

}