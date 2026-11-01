package com.finnexa.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "sales")
public class Sale {

	public enum PaymentMethod {
		CASH, CARD, UPI, BANK_TRANSFER
	}

	public enum SaleStatus {
		COMPLETED, CANCELLED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String invoiceNumber;

	@NotNull(message = "Total amount is required")
	@DecimalMin(value = "0.00", message = "Total amount cannot be negative")
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@NotNull(message = "Payment method is required")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentMethod paymentMethod;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SaleStatus status = SaleStatus.COMPLETED;

	@Column(nullable = false, updatable = false)
	private LocalDateTime saleDate;

	@OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SaleItem> items = new ArrayList<>();

	public Sale() {
	}

	@PrePersist
	protected void onCreate() {
		if (saleDate == null) {
			saleDate = LocalDateTime.now();
		}
	}

	public void addItem(SaleItem item) {
		items.add(item);
		item.setSale(this);
	}

	public void removeItem(SaleItem item) {
		items.remove(item);
		item.setSale(null);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public SaleStatus getStatus() {
		return status;
	}

	public void setStatus(SaleStatus status) {
		this.status = status;
	}

	public LocalDateTime getSaleDate() {
		return saleDate;
	}

	public List<SaleItem> getItems() {
		return items;
	}

}