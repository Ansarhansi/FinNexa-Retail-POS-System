package com.finnexa.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PurchaseRequest {

	@NotNull(message = "Supplier ID is required")
	@Positive(message = "Supplier ID must be positive")
	private Long supplierId;

	@NotEmpty(message = "A purchase must contain at least one item")
	@Valid
	private List<PurchaseItemRequest> items;

	public PurchaseRequest() {
	}

	public Long getSupplierId() {
		return supplierId;
	}

	public void setSupplierId(Long supplierId) {
		this.supplierId = supplierId;
	}

	public List<PurchaseItemRequest> getItems() {
		return items;
	}

	public void setItems(List<PurchaseItemRequest> items) {
		this.items = items;
	}

	public static class PurchaseItemRequest {

		@NotNull(message = "Product ID is required")
		@Positive(message = "Product ID must be positive")
		private Long productId;

		@NotNull(message = "Quantity is required")
		@Positive(message = "Quantity must be greater than zero")
		private Integer quantity;

		@NotNull(message = "Unit cost is required")
		@DecimalMin(value = "0.01", message = "Unit cost must be greater than zero")
		private BigDecimal unitCost;

		public PurchaseItemRequest() {
		}

		public Long getProductId() {
			return productId;
		}

		public void setProductId(Long productId) {
			this.productId = productId;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}

		public BigDecimal getUnitCost() {
			return unitCost;
		}

		public void setUnitCost(BigDecimal unitCost) {
			this.unitCost = unitCost;
		}
	}

}