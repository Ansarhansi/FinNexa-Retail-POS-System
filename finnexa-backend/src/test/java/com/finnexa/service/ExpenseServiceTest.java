package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.ExpenseRequest;
import com.finnexa.dto.ExpenseResponse;
import com.finnexa.entity.Expense;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.ExpenseRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

	@Mock
	private ExpenseRepository expenseRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private ExpenseService expenseService;

	private ExpenseRequest expenseRequest;
	private Expense expense;

	@BeforeEach
	void setUp() {
		expenseRequest = new ExpenseRequest();
		expenseRequest.setTitle("Office Supplies");
		expenseRequest.setDescription("Monthly office stationery");
		expenseRequest.setAmount(new BigDecimal("500.00"));
		expenseRequest.setCategory(Expense.ExpenseCategory.OFFICE_SUPPLIES);
		expenseRequest.setPaymentMethod(Expense.PaymentMethod.UPI);
		expenseRequest.setExpenseDate(LocalDate.of(2026, 10, 26));

		expense = new Expense();
		expense.setId(1L);
		expense.setTitle("Office Supplies");
		expense.setDescription("Monthly office stationery");
		expense.setAmount(new BigDecimal("500.00"));
		expense.setCategory(Expense.ExpenseCategory.OFFICE_SUPPLIES);
		expense.setPaymentMethod(Expense.PaymentMethod.UPI);
		expense.setExpenseDate(LocalDate.of(2026, 10, 26));
	}

	private void mockExpenseSave() {
		when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
			Expense savedExpense = invocation.getArgument(0);
			if (savedExpense.getId() == null) {
				savedExpense.setId(1L);
			}
			return savedExpense;
		});
	}

// =========================================================
// 1. CREATE EXPENSE TESTS
// =========================================================

	@Test
	void createExpenseShouldCreateExpenseSuccessfully() {
		mockExpenseSave();

		ExpenseResponse response = expenseService.createExpense(expenseRequest);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Office Supplies", response.getTitle());
		assertEquals(new BigDecimal("500.00"), response.getAmount());
		assertEquals(Expense.ExpenseCategory.OFFICE_SUPPLIES, response.getCategory());
		assertEquals(Expense.PaymentMethod.UPI, response.getPaymentMethod());

		verify(expenseRepository).save(any(Expense.class));
	}

	@Test
	void createExpenseShouldTrimTitleAndDescription() {
		expenseRequest.setTitle("  Office Supplies  ");
		expenseRequest.setDescription("  Monthly stationery  ");

		mockExpenseSave();

		ExpenseResponse response = expenseService.createExpense(expenseRequest);

		assertEquals("Office Supplies", response.getTitle());
		assertEquals("Monthly stationery", response.getDescription());
	}

	@Test
	void createExpenseShouldRecordAuditLog() {
		mockExpenseSave();

		expenseService.createExpense(expenseRequest);

		verify(auditLogService).recordAction(eq("EXPENSE_CREATED"), eq("Expense"), eq(1L), contains("Office Supplies"));
	}

	@Test
	void createExpenseShouldIncludeAmountInAuditLog() {
		mockExpenseSave();

		expenseService.createExpense(expenseRequest);

		verify(auditLogService).recordAction(eq("EXPENSE_CREATED"), eq("Expense"), eq(1L), contains("500.00"));
	}

	@Test
	void createExpenseShouldRejectNullRequest() {
		assertThrows(BadRequestException.class, () -> expenseService.createExpense(null));
	}

	@Test
	void createExpenseShouldRejectNullTitle() {
		expenseRequest.setTitle(null);

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectBlankTitle() {
		expenseRequest.setTitle("   ");

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectNullDescription() {
		expenseRequest.setDescription(null);

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectBlankDescription() {
		expenseRequest.setDescription("   ");

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectNullAmount() {
		expenseRequest.setAmount(null);

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectZeroAmount() {
		expenseRequest.setAmount(BigDecimal.ZERO);

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectNegativeAmount() {
		expenseRequest.setAmount(new BigDecimal("-100.00"));

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectNullCategory() {
		expenseRequest.setCategory(null);

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectNullPaymentMethod() {
		expenseRequest.setPaymentMethod(null);

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

	@Test
	void createExpenseShouldRejectNullExpenseDate() {
		expenseRequest.setExpenseDate(null);

		assertThrows(BadRequestException.class, () -> expenseService.createExpense(expenseRequest));
	}

// =========================================================
// 2. GET ALL EXPENSES TESTS
// =========================================================

	@Test
	void getAllExpensesShouldReturnAllExpenses() {
		Expense secondExpense = new Expense();
		secondExpense.setId(2L);
		secondExpense.setTitle("Transport");
		secondExpense.setDescription("Business travel");
		secondExpense.setAmount(new BigDecimal("200.00"));
		secondExpense.setCategory(Expense.ExpenseCategory.TRANSPORT);
		secondExpense.setPaymentMethod(Expense.PaymentMethod.CASH);
		secondExpense.setExpenseDate(LocalDate.of(2026, 10, 25));

		when(expenseRepository.findAll()).thenReturn(List.of(expense, secondExpense));

		List<ExpenseResponse> responses = expenseService.getAllExpenses();

		assertEquals(2, responses.size());
		assertEquals("Office Supplies", responses.get(0).getTitle());
		assertEquals("Transport", responses.get(1).getTitle());
	}

	@Test
	void getAllExpensesShouldReturnEmptyList() {
		when(expenseRepository.findAll()).thenReturn(List.of());

		List<ExpenseResponse> responses = expenseService.getAllExpenses();

		assertNotNull(responses);
		assertTrue(responses.isEmpty());
	}

// =========================================================
// 3. GET EXPENSE BY ID TESTS
// =========================================================

	@Test
	void getExpenseByIdShouldReturnExpense() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

		ExpenseResponse response = expenseService.getExpenseById(1L);

		assertEquals(1L, response.getId());
		assertEquals("Office Supplies", response.getTitle());
		assertEquals(new BigDecimal("500.00"), response.getAmount());
	}

	@Test
	void getExpenseByIdShouldRejectNullId() {
		assertThrows(BadRequestException.class, () -> expenseService.getExpenseById(null));
	}

	@Test
	void getExpenseByIdShouldRejectZeroId() {
		assertThrows(BadRequestException.class, () -> expenseService.getExpenseById(0L));
	}

	@Test
	void getExpenseByIdShouldRejectNegativeId() {
		assertThrows(BadRequestException.class, () -> expenseService.getExpenseById(-1L));
	}

	@Test
	void getExpenseByIdShouldRejectMissingExpense() {
		when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> expenseService.getExpenseById(999L));
	}

// =========================================================
// 4. CATEGORY FILTER TESTS
// =========================================================

	@Test
	void getExpensesByCategoryShouldReturnMatchingExpenses() {
		when(expenseRepository.findByCategory(Expense.ExpenseCategory.OFFICE_SUPPLIES)).thenReturn(List.of(expense));

		List<ExpenseResponse> responses = expenseService.getExpensesByCategory(Expense.ExpenseCategory.OFFICE_SUPPLIES);

		assertEquals(1, responses.size());
		assertEquals(Expense.ExpenseCategory.OFFICE_SUPPLIES, responses.get(0).getCategory());
	}

	@Test
	void getExpensesByCategoryShouldReturnEmptyList() {
		when(expenseRepository.findByCategory(Expense.ExpenseCategory.RENT)).thenReturn(List.of());

		List<ExpenseResponse> responses = expenseService.getExpensesByCategory(Expense.ExpenseCategory.RENT);

		assertTrue(responses.isEmpty());
	}

	@Test
	void getExpensesByCategoryShouldRejectNullCategory() {
		assertThrows(BadRequestException.class, () -> expenseService.getExpensesByCategory(null));
	}

// =========================================================
// 5. DATE RANGE FILTER TESTS
// =========================================================

	@Test
	void getExpensesBetweenShouldReturnExpensesWithinRange() {
		LocalDate startDate = LocalDate.of(2026, 10, 1);
		LocalDate endDate = LocalDate.of(2026, 10, 31);

		when(expenseRepository.findByExpenseDateBetween(startDate, endDate)).thenReturn(List.of(expense));

		List<ExpenseResponse> responses = expenseService.getExpensesBetween(startDate, endDate);

		assertEquals(1, responses.size());

		verify(expenseRepository).findByExpenseDateBetween(startDate, endDate);
	}

	@Test
	void getExpensesBetweenShouldReturnEmptyList() {
		LocalDate startDate = LocalDate.of(2026, 9, 1);
		LocalDate endDate = LocalDate.of(2026, 9, 30);

		when(expenseRepository.findByExpenseDateBetween(startDate, endDate)).thenReturn(List.of());

		List<ExpenseResponse> responses = expenseService.getExpensesBetween(startDate, endDate);

		assertTrue(responses.isEmpty());
	}

	@Test
	void getExpensesBetweenShouldAcceptSameStartAndEndDate() {
		LocalDate date = LocalDate.of(2026, 10, 26);

		when(expenseRepository.findByExpenseDateBetween(date, date)).thenReturn(List.of(expense));

		List<ExpenseResponse> responses = expenseService.getExpensesBetween(date, date);

		assertEquals(1, responses.size());
	}

	@Test
	void getExpensesBetweenShouldRejectNullStartDate() {
		assertThrows(BadRequestException.class,
				() -> expenseService.getExpensesBetween(null, LocalDate.of(2026, 10, 31)));
	}

	@Test
	void getExpensesBetweenShouldRejectNullEndDate() {
		assertThrows(BadRequestException.class,
				() -> expenseService.getExpensesBetween(LocalDate.of(2026, 10, 1), null));
	}

	@Test
	void getExpensesBetweenShouldRejectBothNullDates() {
		assertThrows(BadRequestException.class, () -> expenseService.getExpensesBetween(null, null));
	}

	@Test
	void getExpensesBetweenShouldRejectReversedDateRange() {
		assertThrows(BadRequestException.class,
				() -> expenseService.getExpensesBetween(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)));
	}

// =========================================================
// 6. SEARCH BY TITLE TESTS
// =========================================================

	@Test
	void searchExpensesShouldReturnMatchingExpenses() {
		when(expenseRepository.findByTitleContainingIgnoreCase("Office")).thenReturn(List.of(expense));

		List<ExpenseResponse> responses = expenseService.searchExpenses("Office");

		assertEquals(1, responses.size());
		assertEquals("Office Supplies", responses.get(0).getTitle());
	}

	@Test
	void searchExpensesShouldTrimSearchText() {
		when(expenseRepository.findByTitleContainingIgnoreCase("Office")).thenReturn(List.of(expense));

		List<ExpenseResponse> responses = expenseService.searchExpenses("  Office  ");

		assertEquals(1, responses.size());

		verify(expenseRepository).findByTitleContainingIgnoreCase("Office");
	}

	@Test
	void searchExpensesShouldReturnEmptyList() {
		when(expenseRepository.findByTitleContainingIgnoreCase("Unknown")).thenReturn(List.of());

		List<ExpenseResponse> responses = expenseService.searchExpenses("Unknown");

		assertTrue(responses.isEmpty());
	}

	@Test
	void searchExpensesShouldRejectNullTitle() {
		assertThrows(BadRequestException.class, () -> expenseService.searchExpenses(null));
	}

	@Test
	void searchExpensesShouldRejectBlankTitle() {
		assertThrows(BadRequestException.class, () -> expenseService.searchExpenses("   "));
	}

// =========================================================
// 7. UPDATE EXPENSE TESTS
// =========================================================

	@Test
	void updateExpenseShouldUpdateExpenseSuccessfully() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
		mockExpenseSave();

		expenseRequest.setTitle("Updated Office Supplies");
		expenseRequest.setAmount(new BigDecimal("750.00"));

		ExpenseResponse response = expenseService.updateExpense(1L, expenseRequest);

		assertEquals("Updated Office Supplies", response.getTitle());
		assertEquals(new BigDecimal("750.00"), response.getAmount());

		verify(expenseRepository).save(expense);
	}

	@Test
	void updateExpenseShouldRecordAuditLog() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
		mockExpenseSave();

		expenseService.updateExpense(1L, expenseRequest);

		verify(auditLogService).recordAction(eq("EXPENSE_UPDATED"), eq("Expense"), eq(1L), contains("Office Supplies"));
	}

	@Test
	void updateExpenseShouldRejectNullId() {
		assertThrows(BadRequestException.class, () -> expenseService.updateExpense(null, expenseRequest));
	}

	@Test
	void updateExpenseShouldRejectZeroId() {
		assertThrows(BadRequestException.class, () -> expenseService.updateExpense(0L, expenseRequest));
	}

	@Test
	void updateExpenseShouldRejectNegativeId() {
		assertThrows(BadRequestException.class, () -> expenseService.updateExpense(-1L, expenseRequest));
	}

	@Test
	void updateExpenseShouldRejectMissingExpense() {
		when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> expenseService.updateExpense(999L, expenseRequest));
	}

	@Test
	void updateExpenseShouldRejectNullRequest() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

		assertThrows(BadRequestException.class, () -> expenseService.updateExpense(1L, null));
	}

	@Test
	void updateExpenseShouldRejectInvalidAmount() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
		expenseRequest.setAmount(BigDecimal.ZERO);

		assertThrows(BadRequestException.class, () -> expenseService.updateExpense(1L, expenseRequest));
	}

// =========================================================
// 8. DELETE EXPENSE TESTS
// =========================================================

	@Test
	void deleteExpenseShouldDeleteExpenseSuccessfully() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

		expenseService.deleteExpense(1L);

		verify(expenseRepository).delete(expense);
	}

	@Test
	void deleteExpenseShouldRecordAuditLog() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

		expenseService.deleteExpense(1L);

		verify(auditLogService).recordAction(eq("EXPENSE_DELETED"), eq("Expense"), eq(1L), contains("Office Supplies"));
	}

	@Test
	void deleteExpenseShouldIncludeAmountInAuditLog() {
		when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

		expenseService.deleteExpense(1L);

		verify(auditLogService).recordAction(eq("EXPENSE_DELETED"), eq("Expense"), eq(1L), contains("500.00"));
	}

	@Test
	void deleteExpenseShouldRejectNullId() {
		assertThrows(BadRequestException.class, () -> expenseService.deleteExpense(null));
	}

	@Test
	void deleteExpenseShouldRejectZeroId() {
		assertThrows(BadRequestException.class, () -> expenseService.deleteExpense(0L));
	}

	@Test
	void deleteExpenseShouldRejectNegativeId() {
		assertThrows(BadRequestException.class, () -> expenseService.deleteExpense(-1L));
	}

	@Test
	void deleteExpenseShouldRejectMissingExpense() {
		when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> expenseService.deleteExpense(999L));
	}

}
