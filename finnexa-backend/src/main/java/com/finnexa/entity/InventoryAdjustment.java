package com.finnexa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Product is required")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@NotNull(message = "Quantity change is required")
	@Column(name = "quantity_change", nullable = false)
	private Integer quantityChange;

	@NotNull(message = "Previous quantity is required")
	@Column(name = "previous_quantity", nullable = false)
	private Integer previousQuantity;

	@NotNull(message = "New quantity is required")
	@Column(name = "new_quantity", nullable = false)
	private Integer newQuantity;

	@NotBlank(message = "Reason is required")
	@Size(max = 255)
	@Column(nullable = false, length = 255)
	private String reason;

	@Column(name = "adjusted_at", nullable = false, updatable = false)
	private LocalDateTime adjustedAt;

	public InventoryAdjustment() {
	}

	@PrePersist
	public void prePersist() {
		adjustedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public Integer getQuantityChange() {
		return quantityChange;
	}

	public void setQuantityChange(Integer quantityChange) {
		this.quantityChange = quantityChange;
	}

	public Integer getPreviousQuantity() {
		return previousQuantity;
	}

	public void setPreviousQuantity(Integer previousQuantity) {
		this.previousQuantity = previousQuantity;
	}

	public Integer getNewQuantity() {
		return newQuantity;
	}

	public void setNewQuantity(Integer newQuantity) {
		this.newQuantity = newQuantity;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDateTime getAdjustedAt() {
		return adjustedAt;
	}

}
