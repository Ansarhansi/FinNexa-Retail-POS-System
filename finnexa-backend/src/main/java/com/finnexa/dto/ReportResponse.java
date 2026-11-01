package com.finnexa.dto;

import java.math.BigDecimal;

public class ReportResponse {

	private BigDecimal totalRevenue;
	private long totalSales;
	private BigDecimal averageSaleValue;
	private long cancelledSales;

	public ReportResponse() {
	}

	public ReportResponse(BigDecimal totalRevenue, long totalSales, BigDecimal averageSaleValue, long cancelledSales) {
		this.totalRevenue = totalRevenue;
		this.totalSales = totalSales;
		this.averageSaleValue = averageSaleValue;
		this.cancelledSales = cancelledSales;
	}

	public BigDecimal getTotalRevenue() {
		return totalRevenue;
	}

	public void setTotalRevenue(BigDecimal totalRevenue) {
		this.totalRevenue = totalRevenue;
	}

	public long getTotalSales() {
		return totalSales;
	}

	public void setTotalSales(long totalSales) {
		this.totalSales = totalSales;
	}

	public BigDecimal getAverageSaleValue() {
		return averageSaleValue;
	}

	public void setAverageSaleValue(BigDecimal averageSaleValue) {
		this.averageSaleValue = averageSaleValue;
	}

	public long getCancelledSales() {
		return cancelledSales;
	}

	public void setCancelledSales(long cancelledSales) {
		this.cancelledSales = cancelledSales;
	}
}