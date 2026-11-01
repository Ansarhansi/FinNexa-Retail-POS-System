package com.finnexa.dto;

import java.math.BigDecimal;

public class InventorySummaryResponse {
	private long totalProducts;
	private long lowStockProducts;
	private long outOfStockProducts;
	private BigDecimal totalInventorySellingValue;

	public InventorySummaryResponse() {
	}

	public InventorySummaryResponse(long totalProducts, long lowStockProducts, long outOfStockProducts,
			BigDecimal totalInventorySellingValue) {
		this.totalProducts = totalProducts;
		this.lowStockProducts = lowStockProducts;
		this.outOfStockProducts = outOfStockProducts;
		this.totalInventorySellingValue = totalInventorySellingValue;
	}

	public long getTotalProducts() {
		return totalProducts;
	}

	public void setTotalProducts(long totalProducts) {
		this.totalProducts = totalProducts;
	}

	public long getLowStockProducts() {
		return lowStockProducts;
	}

	public void setLowStockProducts(long lowStockProducts) {
		this.lowStockProducts = lowStockProducts;
	}

	public long getOutOfStockProducts() {
		return outOfStockProducts;
	}

	public void setOutOfStockProducts(long outOfStockProducts) {
		this.outOfStockProducts = outOfStockProducts;
	}

	public BigDecimal getTotalInventorySellingValue() {
		return totalInventorySellingValue;
	}

	public void setTotalInventorySellingValue(BigDecimal totalInventorySellingValue) {
		this.totalInventorySellingValue = totalInventorySellingValue;
	}
}