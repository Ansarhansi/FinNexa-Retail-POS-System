package com.finnexa.dto;

import java.time.LocalDateTime;

public class InventoryAdjustmentResponse {
	private Long id;
	private Long productId;
	private String productName;
	private Integer quantityChange;
	private Integer previousQuantity;
	private Integer newQuantity;
	private String reason;
	private LocalDateTime adjustedAt;

	public InventoryAdjustmentResponse() {
	}

	public InventoryAdjustmentResponse(Long id, Long productId, String productName, Integer quantityChange,
			Integer previousQuantity, Integer newQuantity, String reason, LocalDateTime adjustedAt) {
		this.id = id;
		this.productId = productId;
		this.productName = productName;
		this.quantityChange = quantityChange;
		this.previousQuantity = previousQuantity;
		this.newQuantity = newQuantity;
		this.reason = reason;
		this.adjustedAt = adjustedAt;
	}

	public Long getId() {
		return id;
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public Integer getQuantityChange() {
		return quantityChange;
	}

	public Integer getPreviousQuantity() {
		return previousQuantity;
	}

	public Integer getNewQuantity() {
		return newQuantity;
	}

	public String getReason() {
		return reason;
	}

	public LocalDateTime getAdjustedAt() {
		return adjustedAt;
	}
}