
package com.finnexa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finnexa.dto.ExpenseRequest;
import com.finnexa.dto.ExpenseResponse;
import com.finnexa.entity.Expense;
import com.finnexa.service.ExpenseService;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ExpenseService expenseService;

	private ExpenseRequest expenseRequest;
	private ExpenseResponse expenseResponse;

	@BeforeEach
	void setUp() {
		expenseRequest = new ExpenseRequest();
		expenseRequest.setTitle("Office Rent");
		expenseRequest.setDescription("Monthly office rent payment");
		expenseRequest.setAmount(new BigDecimal("15000.00"));
		expenseRequest.setCategory(Expense.ExpenseCategory.RENT);
		expenseRequest.setPaymentMethod(Expense.PaymentMethod.UPI);
		expenseRequest.setExpenseDate(LocalDate.of(2026, 10, 1));

		expenseResponse = new ExpenseResponse() {
			@Override
			public Long getId() {
				return 1L;
			}

			@Override
			public String getTitle() {
				return "Office Rent";
			}

			@Override
			public String getDescription() {
				return "Monthly office rent payment";
			}

			@Override
			public BigDecimal getAmount() {
				return new BigDecimal("15000.00");
			}

			@Override
			public Expense.ExpenseCategory getCategory() {
				return Expense.ExpenseCategory.RENT;
			}

			@Override
			public Expense.PaymentMethod getPaymentMethod() {
				return Expense.PaymentMethod.UPI;
			}

			@Override
			public LocalDate getExpenseDate() {
				return LocalDate.of(2026, 10, 1);
			}

			@Override
			public LocalDateTime getCreatedAt() {
				return LocalDateTime.of(2026, 10, 1, 10, 0);
			}

			@Override
			public LocalDateTime getUpdatedAt() {
				return LocalDateTime.of(2026, 10, 1, 10, 0);
			}
		};
	}

	@Test
	void createExpense_ShouldReturnCreated() throws Exception {
		when(expenseService.createExpense(any(ExpenseRequest.class))).thenReturn(expenseResponse);

		mockMvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(expenseRequest))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.title").value("Office Rent"))
				.andExpect(jsonPath("$.amount").value(15000.00)).andExpect(jsonPath("$.category").value("RENT"));

		verify(expenseService).createExpense(any(ExpenseRequest.class));
	}

	@Test
	void getAllExpenses_ShouldReturnOk() throws Exception {
		when(expenseService.getAllExpenses()).thenReturn(List.of(expenseResponse));

		mockMvc.perform(get("/api/expenses")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].title").value("Office Rent"));

		verify(expenseService).getAllExpenses();
	}

	@Test
	void getExpenseById_ShouldReturnOk() throws Exception {
		when(expenseService.getExpenseById(1L)).thenReturn(expenseResponse);

		mockMvc.perform(get("/api/expenses/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.title").value("Office Rent"));

		verify(expenseService).getExpenseById(1L);
	}

	@Test
	void getExpensesByCategory_ShouldReturnOk() throws Exception {
		when(expenseService.getExpensesByCategory(Expense.ExpenseCategory.RENT)).thenReturn(List.of(expenseResponse));

		mockMvc.perform(get("/api/expenses/category").param("category", "RENT")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].category").value("RENT"))
				.andExpect(jsonPath("$[0].title").value("Office Rent"));

		verify(expenseService).getExpensesByCategory(Expense.ExpenseCategory.RENT);
	}

	@Test
	void getExpensesBetween_ShouldReturnOk() throws Exception {
		LocalDate startDate = LocalDate.of(2026, 10, 1);
		LocalDate endDate = LocalDate.of(2026, 10, 31);

		when(expenseService.getExpensesBetween(startDate, endDate)).thenReturn(List.of(expenseResponse));

		mockMvc.perform(get("/api/expenses/date-range").param("startDate", "2026-10-01").param("endDate", "2026-10-31"))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Office Rent"));

		verify(expenseService).getExpensesBetween(startDate, endDate);
	}

	@Test
	void searchExpenses_ShouldReturnOk() throws Exception {
		when(expenseService.searchExpenses("Office")).thenReturn(List.of(expenseResponse));

		mockMvc.perform(get("/api/expenses/search").param("title", "Office")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Office Rent"));

		verify(expenseService).searchExpenses("Office");
	}

	@Test
	void updateExpense_ShouldReturnOk() throws Exception {
		when(expenseService.updateExpense(org.mockito.ArgumentMatchers.eq(1L), any(ExpenseRequest.class)))
				.thenReturn(expenseResponse);

		mockMvc.perform(put("/api/expenses/1").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(expenseRequest))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.title").value("Office Rent"));

		verify(expenseService).updateExpense(org.mockito.ArgumentMatchers.eq(1L), any(ExpenseRequest.class));
	}

	@Test
	void deleteExpense_ShouldReturnNoContent() throws Exception {
		doNothing().when(expenseService).deleteExpense(1L);

		mockMvc.perform(delete("/api/expenses/1")).andExpect(status().isNoContent());

		verify(expenseService).deleteExpense(1L);
	}
}
