
package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.DashboardResponse;
import com.finnexa.dto.FinancialReportRequest;
import com.finnexa.dto.FinancialSummaryResponse;
import com.finnexa.entity.Expense;
import com.finnexa.entity.Product;
import com.finnexa.entity.Purchase;
import com.finnexa.entity.Sale;
import com.finnexa.repository.CustomerRepository;
import com.finnexa.repository.ExpenseRepository;
import com.finnexa.repository.ProductRepository;
import com.finnexa.repository.PurchaseRepository;
import com.finnexa.repository.SaleRepository;
import com.finnexa.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

	@Mock
	private SaleRepository saleRepository;

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private PurchaseRepository purchaseRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private SupplierRepository supplierRepository;

	@InjectMocks
	private FinancialReportService financialReportService;

	private LocalDate startDate;
	private LocalDate endDate;

	@BeforeEach
	void setUp() {
		startDate = LocalDate.of(2026, 1, 1);
		endDate = LocalDate.of(2026, 1, 31);
	}

	// ---------------------------------------------------------
	// Financial summary tests
	// ---------------------------------------------------------

	@Test
	void getFinancialSummary_shouldCalculateFinancialTotals() {
		FinancialReportRequest request = createRequest(startDate, endDate);

		Sale completedSale1 = createSale("100.00", Sale.SaleStatus.COMPLETED);
		Sale completedSale2 = createSale("200.00", Sale.SaleStatus.COMPLETED);
		Sale cancelledSale = createSale("500.00", Sale.SaleStatus.CANCELLED);
		Sale saleWithNullAmount = createSale(null, Sale.SaleStatus.COMPLETED);

		Expense expense1 = createExpense("30.00");
		Expense expense2 = createExpense("20.00");
		Expense expenseWithNullAmount = createExpense(null);

		Purchase receivedPurchase1 = createPurchase("80.00", Purchase.PurchaseStatus.RECEIVED);
		Purchase receivedPurchase2 = createPurchase("120.00", Purchase.PurchaseStatus.RECEIVED);
		Purchase pendingPurchase = createPurchase("300.00", Purchase.PurchaseStatus.PENDING);
		Purchase cancelledPurchase = createPurchase("400.00", Purchase.PurchaseStatus.CANCELLED);

		when(saleRepository.findBySaleDateBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
				.thenReturn(List.of(completedSale1, completedSale2, cancelledSale, saleWithNullAmount));

		when(expenseRepository.findByExpenseDateBetween(startDate, endDate))
				.thenReturn(List.of(expense1, expense2, expenseWithNullAmount));

		when(purchaseRepository.findByPurchaseDateBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
				.thenReturn(List.of(receivedPurchase1, receivedPurchase2, pendingPurchase, cancelledPurchase));

		FinancialSummaryResponse response = financialReportService.getFinancialSummary(request);

		assertEquals(startDate, response.getStartDate());
		assertEquals(endDate, response.getEndDate());
		assertEquals(new BigDecimal("300.00"), response.getTotalSalesRevenue());
		assertEquals(new BigDecimal("50.00"), response.getTotalExpenses());
		assertEquals(new BigDecimal("200.00"), response.getTotalReceivedPurchases());
		assertEquals(new BigDecimal("250.00"), response.getEstimatedProfitBeforeCostOfGoodsSold());

		verify(saleRepository).findBySaleDateBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
		verify(expenseRepository).findByExpenseDateBetween(startDate, endDate);
		verify(purchaseRepository).findByPurchaseDateBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
	}

	@Test
	void getFinancialSummary_shouldReturnZeroTotalsWhenRepositoriesReturnEmptyLists() {
		FinancialReportRequest request = createRequest(startDate, endDate);

		when(saleRepository.findBySaleDateBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
				.thenReturn(List.of());

		when(expenseRepository.findByExpenseDateBetween(startDate, endDate)).thenReturn(List.of());

		when(purchaseRepository.findByPurchaseDateBetween(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
				.thenReturn(List.of());

		FinancialSummaryResponse response = financialReportService.getFinancialSummary(request);

		assertEquals(BigDecimal.ZERO, response.getTotalSalesRevenue());
		assertEquals(BigDecimal.ZERO, response.getTotalExpenses());
		assertEquals(BigDecimal.ZERO, response.getTotalReceivedPurchases());
		assertEquals(BigDecimal.ZERO, response.getEstimatedProfitBeforeCostOfGoodsSold());
	}

	@Test
	void getFinancialSummary_shouldAllowSameStartAndEndDate() {
		FinancialReportRequest request = createRequest(startDate, startDate);

		when(saleRepository.findBySaleDateBetween(startDate.atStartOfDay(), startDate.atTime(LocalTime.MAX)))
				.thenReturn(List.of());

		when(expenseRepository.findByExpenseDateBetween(startDate, startDate)).thenReturn(List.of());

		when(purchaseRepository.findByPurchaseDateBetween(startDate.atStartOfDay(), startDate.atTime(LocalTime.MAX)))
				.thenReturn(List.of());

		FinancialSummaryResponse response = financialReportService.getFinancialSummary(request);

		assertEquals(startDate, response.getStartDate());
		assertEquals(startDate, response.getEndDate());
		assertEquals(BigDecimal.ZERO, response.getTotalSalesRevenue());
	}

	@Test
	void getFinancialSummary_shouldThrowExceptionWhenRequestIsNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> financialReportService.getFinancialSummary(null));

		assertEquals("Financial report request is required.", exception.getMessage());
	}

	@Test
	void getFinancialSummary_shouldThrowExceptionWhenStartDateIsNull() {
		FinancialReportRequest request = createRequest(null, endDate);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> financialReportService.getFinancialSummary(request));

		assertEquals("Start date and end date are required.", exception.getMessage());
	}

	@Test
	void getFinancialSummary_shouldThrowExceptionWhenEndDateIsNull() {
		FinancialReportRequest request = createRequest(startDate, null);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> financialReportService.getFinancialSummary(request));

		assertEquals("Start date and end date are required.", exception.getMessage());
	}

	@Test
	void getFinancialSummary_shouldThrowExceptionWhenStartDateIsAfterEndDate() {
		FinancialReportRequest request = createRequest(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> financialReportService.getFinancialSummary(request));

		assertEquals("Start date cannot be after end date.", exception.getMessage());
	}

	// ---------------------------------------------------------
	// Dashboard tests
	// ---------------------------------------------------------

	@Test
	void getDashboard_shouldCalculateCountsAndFinancialTotals() {
		when(productRepository.count()).thenReturn(10L);
		when(customerRepository.count()).thenReturn(20L);
		when(supplierRepository.count()).thenReturn(5L);
		when(saleRepository.count()).thenReturn(30L);
		when(purchaseRepository.count()).thenReturn(15L);
		when(expenseRepository.count()).thenReturn(8L);

		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of(new Product(), new Product()));

		Sale completedSale1 = createSale("1000.00", Sale.SaleStatus.COMPLETED);
		Sale completedSale2 = createSale("500.00", Sale.SaleStatus.COMPLETED);
		Sale cancelledSale = createSale("200.00", Sale.SaleStatus.CANCELLED);
		Sale saleWithNullAmount = createSale(null, Sale.SaleStatus.COMPLETED);

		Expense expense1 = createExpense("100.00");
		Expense expense2 = createExpense("50.00");
		Expense expenseWithNullAmount = createExpense(null);

		when(saleRepository.findAll())
				.thenReturn(List.of(completedSale1, completedSale2, cancelledSale, saleWithNullAmount));

		when(expenseRepository.findAll()).thenReturn(List.of(expense1, expense2, expenseWithNullAmount));

		DashboardResponse response = financialReportService.getDashboard();

		assertEquals(10L, response.getTotalProducts());
		assertEquals(20L, response.getTotalCustomers());
		assertEquals(5L, response.getTotalSuppliers());
		assertEquals(30L, response.getTotalSales());
		assertEquals(15L, response.getTotalPurchases());
		assertEquals(8L, response.getTotalExpenses());
		assertEquals(2, response.getLowStockProducts());

		assertEquals(new BigDecimal("1500.00"), response.getTotalSalesRevenue());

		assertEquals(new BigDecimal("150.00"), response.getTotalExpensesAmount());

		assertEquals(new BigDecimal("1350.00"), response.getEstimatedProfitBeforeCostOfGoodsSold());
	}

	@Test
	void getDashboard_shouldReturnZeroFinancialTotalsWhenNoRecordsExist() {
		when(productRepository.count()).thenReturn(0L);
		when(customerRepository.count()).thenReturn(0L);
		when(supplierRepository.count()).thenReturn(0L);
		when(saleRepository.count()).thenReturn(0L);
		when(purchaseRepository.count()).thenReturn(0L);
		when(expenseRepository.count()).thenReturn(0L);

		when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of());

		when(saleRepository.findAll()).thenReturn(List.of());
		when(expenseRepository.findAll()).thenReturn(List.of());

		DashboardResponse response = financialReportService.getDashboard();

		assertEquals(0L, response.getTotalProducts());
		assertEquals(0L, response.getTotalCustomers());
		assertEquals(0L, response.getTotalSuppliers());
		assertEquals(0L, response.getTotalSales());
		assertEquals(0L, response.getTotalPurchases());
		assertEquals(0L, response.getTotalExpenses());
		assertEquals(0, response.getLowStockProducts());

		assertEquals(BigDecimal.ZERO, response.getTotalSalesRevenue());
		assertEquals(BigDecimal.ZERO, response.getTotalExpensesAmount());
		assertEquals(BigDecimal.ZERO, response.getEstimatedProfitBeforeCostOfGoodsSold());
	}

	// ---------------------------------------------------------
	// Test helper methods
	// ---------------------------------------------------------

	private FinancialReportRequest createRequest(LocalDate startDate, LocalDate endDate) {

		FinancialReportRequest request = new FinancialReportRequest();
		request.setStartDate(startDate);
		request.setEndDate(endDate);
		return request;
	}

	private Sale createSale(String amount, Sale.SaleStatus status) {
		Sale sale = new Sale();

		if (amount != null) {
			sale.setTotalAmount(new BigDecimal(amount));
		} else {
			sale.setTotalAmount(null);
		}

		sale.setStatus(status);
		sale.setInvoiceNumber("INV-" + System.nanoTime());
		sale.setPaymentMethod(Sale.PaymentMethod.CASH);

		return sale;
	}

	private Expense createExpense(String amount) {
		Expense expense = new Expense();
		expense.setTitle("Test expense");
		expense.setDescription("Financial report test expense");
		expense.setAmount(amount == null ? null : new BigDecimal(amount));
		expense.setCategory(Expense.ExpenseCategory.OTHER);
		expense.setPaymentMethod(Expense.PaymentMethod.CASH);
		expense.setExpenseDate(startDate);

		return expense;
	}

	private Purchase createPurchase(String amount, Purchase.PurchaseStatus status) {

		Purchase purchase = new Purchase();

		if (amount != null) {
			purchase.setTotalAmount(new BigDecimal(amount));
		} else {
			purchase.setTotalAmount(null);
		}

		purchase.setStatus(status);
		purchase.setPurchaseNumber("PUR-" + System.nanoTime());

		return purchase;
	}
}
