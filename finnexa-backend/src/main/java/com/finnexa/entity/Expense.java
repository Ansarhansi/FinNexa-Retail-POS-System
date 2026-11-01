package com.finnexa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "expenses")
public class Expense {

	public enum ExpenseCategory {
		RENT, UTILITIES, SALARY, TRANSPORT, OFFICE_SUPPLIES, MAINTENANCE, MARKETING, FOOD, OTHER
	}

	public enum PaymentMethod {
		CASH, CARD, UPI, BANK_TRANSFER
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Expense title is required")
	@Size(max = 150, message = "Title cannot exceed 150 characters")
	@Column(nullable = false, length = 150)
	private String title;

	@NotBlank(message = "Expense description is required")
	@Size(max = 500, message = "Description cannot exceed 500 characters")
	@Column(nullable = false, length = 500)
	private String description;

	@NotNull(message = "Expense amount is required")
	@DecimalMin(value = "0.01", message = "Expense amount must be greater than zero")
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@NotNull(message = "Expense category is required")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ExpenseCategory category;

	@NotNull(message = "Payment method is required")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentMethod paymentMethod;

	@NotNull(message = "Expense date is required")
	@Column(nullable = false)
	private LocalDate expenseDate;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public Expense() {
	}

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;

		if (expenseDate == null) {
			expenseDate = LocalDate.now();
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public ExpenseCategory getCategory() {
		return category;
	}

	public void setCategory(ExpenseCategory category) {
		this.category = category;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public LocalDate getExpenseDate() {
		return expenseDate;
	}

	public void setExpenseDate(LocalDate expenseDate) {
		this.expenseDate = expenseDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

}