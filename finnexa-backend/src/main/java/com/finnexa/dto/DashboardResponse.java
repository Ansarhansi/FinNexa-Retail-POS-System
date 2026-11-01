package com.finnexa.dto;

import java.math.BigDecimal;

public class DashboardResponse {
	private long totalProducts;
	private long totalCustomers;
	private long totalSuppliers;
	private long totalSales;
	private long totalPurchases;
	private long totalExpenses;
	private long lowStockProducts;
	private BigDecimal totalSalesRevenue;
	private BigDecimal totalExpensesAmount;
	private BigDecimal estimatedProfitBeforeCostOfGoodsSold;

	public DashboardResponse() {
	}

	public long getTotalProducts() {
		return totalProducts;
	}

	public void setTotalProducts(long totalProducts) {
		this.totalProducts = totalProducts;
	}

	public long getTotalCustomers() {
		return totalCustomers;
	}

	public void setTotalCustomers(long totalCustomers) {
		this.totalCustomers = totalCustomers;
	}

	public long getTotalSuppliers() {
		return totalSuppliers;
	}

	public void setTotalSuppliers(long totalSuppliers) {
		this.totalSuppliers = totalSuppliers;
	}

	public long getTotalSales() {
		return totalSales;
	}

	public void setTotalSales(long totalSales) {
		this.totalSales = totalSales;
	}

	public long getTotalPurchases() {
		return totalPurchases;
	}

	public void setTotalPurchases(long totalPurchases) {
		this.totalPurchases = totalPurchases;
	}

	public long getTotalExpenses() {
		return totalExpenses;
	}

	public void setTotalExpenses(long totalExpenses) {
		this.totalExpenses = totalExpenses;
	}

	public long getLowStockProducts() {
		return lowStockProducts;
	}

	public void setLowStockProducts(long lowStockProducts) {
		this.lowStockProducts = lowStockProducts;
	}

	public BigDecimal getTotalSalesRevenue() {
		return totalSalesRevenue;
	}

	public void setTotalSalesRevenue(BigDecimal totalSalesRevenue) {
		this.totalSalesRevenue = totalSalesRevenue;
	}

	public BigDecimal getTotalExpensesAmount() {
		return totalExpensesAmount;
	}

	public void setTotalExpensesAmount(BigDecimal totalExpensesAmount) {
		this.totalExpensesAmount = totalExpensesAmount;
	}

	public BigDecimal getEstimatedProfitBeforeCostOfGoodsSold() {
		return estimatedProfitBeforeCostOfGoodsSold;
	}

	public void setEstimatedProfitBeforeCostOfGoodsSold(BigDecimal estimatedProfitBeforeCostOfGoodsSold) {
		this.estimatedProfitBeforeCostOfGoodsSold = estimatedProfitBeforeCostOfGoodsSold;
	}
}