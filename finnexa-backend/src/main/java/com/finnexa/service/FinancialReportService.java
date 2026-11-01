package com.finnexa.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finnexa.dto.DashboardResponse;
import com.finnexa.dto.FinancialReportRequest;
import com.finnexa.dto.FinancialSummaryResponse;
import com.finnexa.entity.Expense;
import com.finnexa.entity.Purchase;
import com.finnexa.entity.Sale;
import com.finnexa.repository.CustomerRepository;
import com.finnexa.repository.ExpenseRepository;
import com.finnexa.repository.ProductRepository;
import com.finnexa.repository.PurchaseRepository;
import com.finnexa.repository.SaleRepository;
import com.finnexa.repository.SupplierRepository;

@Service
@Transactional(readOnly = true)
public class FinancialReportService {
	private static final int LOW_STOCK_THRESHOLD = 5;
	private final SaleRepository saleRepository;
	private final ExpenseRepository expenseRepository;
	private final PurchaseRepository purchaseRepository;
	private final ProductRepository productRepository;
	private final CustomerRepository customerRepository;
	private final SupplierRepository supplierRepository;

	public FinancialReportService(SaleRepository saleRepository, ExpenseRepository expenseRepository,
			PurchaseRepository purchaseRepository, ProductRepository productRepository,
			CustomerRepository customerRepository, SupplierRepository supplierRepository) {
		this.saleRepository = saleRepository;
		this.expenseRepository = expenseRepository;
		this.purchaseRepository = purchaseRepository;
		this.productRepository = productRepository;
		this.customerRepository = customerRepository;
		this.supplierRepository = supplierRepository;
	}

	public FinancialSummaryResponse getFinancialSummary(FinancialReportRequest request) {
		validateDateRange(request);
		LocalDate startDate = request.getStartDate();
		LocalDate endDate = request.getEndDate();
		LocalDateTime startDateTime = startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
		List<Sale> sales = saleRepository.findBySaleDateBetween(startDateTime, endDateTime);
		List<Expense> expenses = expenseRepository.findByExpenseDateBetween(startDate, endDate);
		List<Purchase> purchases = purchaseRepository.findByPurchaseDateBetween(startDateTime, endDateTime);
		BigDecimal totalSalesRevenue = sales.stream().filter(sale -> sale.getStatus() == Sale.SaleStatus.COMPLETED)
				.map(Sale::getTotalAmount).filter(amount -> amount != null).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalExpenses = expenses.stream().map(Expense::getAmount).filter(amount -> amount != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalReceivedPurchases = purchases.stream()
				.filter(purchase -> purchase.getStatus() == Purchase.PurchaseStatus.RECEIVED)
				.map(Purchase::getTotalAmount).filter(amount -> amount != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal estimatedProfitBeforeCostOfGoodsSold = totalSalesRevenue.subtract(totalExpenses);
		return new FinancialSummaryResponse(startDate, endDate, totalSalesRevenue, totalExpenses,
				totalReceivedPurchases, estimatedProfitBeforeCostOfGoodsSold);
	}

	public DashboardResponse getDashboard() {
		DashboardResponse dashboard = new DashboardResponse();
		dashboard.setTotalProducts(productRepository.count());
		dashboard.setTotalCustomers(customerRepository.count());
		dashboard.setTotalSuppliers(supplierRepository.count());
		dashboard.setTotalSales(saleRepository.count());
		dashboard.setTotalPurchases(purchaseRepository.count());
		dashboard.setTotalExpenses(expenseRepository.count());
		dashboard.setLowStockProducts(productRepository.findByQuantityLessThanEqual(LOW_STOCK_THRESHOLD).size());
		List<Sale> sales = saleRepository.findAll();
		List<Expense> expenses = expenseRepository.findAll();
		BigDecimal totalSalesRevenue = sales.stream().filter(sale -> sale.getStatus() == Sale.SaleStatus.COMPLETED)
				.map(Sale::getTotalAmount).filter(amount -> amount != null).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalExpensesAmount = expenses.stream().map(Expense::getAmount).filter(amount -> amount != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		dashboard.setTotalSalesRevenue(totalSalesRevenue);
		dashboard.setTotalExpensesAmount(totalExpensesAmount);
		dashboard.setEstimatedProfitBeforeCostOfGoodsSold(totalSalesRevenue.subtract(totalExpensesAmount));
		return dashboard;
	}

	private void validateDateRange(FinancialReportRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Financial report request is required.");
		}
		if (request.getStartDate() == null || request.getEndDate() == null) {
			throw new IllegalArgumentException("Start date and end date are required.");
		}
		if (request.getStartDate().isAfter(request.getEndDate())) {
			throw new IllegalArgumentException("Start date cannot be after end date.");
		}
	}
}