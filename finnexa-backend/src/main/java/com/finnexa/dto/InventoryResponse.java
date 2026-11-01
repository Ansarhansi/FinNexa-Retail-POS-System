package com.finnexa.dto;

import java.math.BigDecimal;

public class InventoryResponse {
	private Long productId;
	private String productName;
	private String sku;
	private Integer availableQuantity;
	private BigDecimal unitPrice;
	private boolean lowStock;

	public InventoryResponse() {
	}

	public InventoryResponse(Long productId, String productName, String sku, Integer availableQuantity,
			BigDecimal unitPrice, boolean lowStock) {
		this.productId = productId;
		this.productName = productName;
		this.sku = sku;
		this.availableQuantity = availableQuantity;
		this.unitPrice = unitPrice;
		this.lowStock = lowStock;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public Integer getAvailableQuantity() {
		return availableQuantity;
	}

	public void setAvailableQuantity(Integer availableQuantity) {
		this.availableQuantity = availableQuantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public boolean isLowStock() {
		return lowStock;
	}

	public void setLowStock(boolean lowStock) {
		this.lowStock = lowStock;
	}
}