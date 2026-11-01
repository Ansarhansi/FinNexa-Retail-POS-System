package com.finnexa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialSummaryResponse {
	private LocalDate startDate;
	private LocalDate endDate;
	private BigDecimal totalSalesRevenue;
	private BigDecimal totalExpenses;
	private BigDecimal totalReceivedPurchases;
	private BigDecimal estimatedProfitBeforeCostOfGoodsSold;

	public FinancialSummaryResponse() {
	}

	public FinancialSummaryResponse(LocalDate startDate, LocalDate endDate, BigDecimal totalSalesRevenue,
			BigDecimal totalExpenses, BigDecimal totalReceivedPurchases,
			BigDecimal estimatedProfitBeforeCostOfGoodsSold) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.totalSalesRevenue = totalSalesRevenue;
		this.totalExpenses = totalExpenses;
		this.totalReceivedPurchases = totalReceivedPurchases;
		this.estimatedProfitBeforeCostOfGoodsSold = estimatedProfitBeforeCostOfGoodsSold;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public BigDecimal getTotalSalesRevenue() {
		return totalSalesRevenue;
	}

	public void setTotalSalesRevenue(BigDecimal totalSalesRevenue) {
		this.totalSalesRevenue = totalSalesRevenue;
	}

	public BigDecimal getTotalExpenses() {
		return totalExpenses;
	}

	public void setTotalExpenses(BigDecimal totalExpenses) {
		this.totalExpenses = totalExpenses;
	}

	public BigDecimal getTotalReceivedPurchases() {
		return totalReceivedPurchases;
	}

	public void setTotalReceivedPurchases(BigDecimal totalReceivedPurchases) {
		this.totalReceivedPurchases = totalReceivedPurchases;
	}

	public BigDecimal getEstimatedProfitBeforeCostOfGoodsSold() {
		return estimatedProfitBeforeCostOfGoodsSold;
	}

	public void setEstimatedProfitBeforeCostOfGoodsSold(BigDecimal estimatedProfitBeforeCostOfGoodsSold) {
		this.estimatedProfitBeforeCostOfGoodsSold = estimatedProfitBeforeCostOfGoodsSold;
	}
}