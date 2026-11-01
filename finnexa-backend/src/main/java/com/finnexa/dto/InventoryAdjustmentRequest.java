package com.finnexa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public class InventoryAdjustmentRequest {
	@NotNull(message = "Product ID is required")
	@Min(value = 1, message = "Product ID must be greater than zero")
	private Long productId;
	@NotNull(message = "Quantity change is required")
	private Integer quantityChange;
	@NotBlank(message = "Reason is required")
	@Size(max = 255, message = "Reason cannot exceed 255 characters")
	private String reason;

	public InventoryAdjustmentRequest() {
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Integer getQuantityChange() {
		return quantityChange;
	}

	public void setQuantityChange(Integer quantityChange) {
		this.quantityChange = quantityChange;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}