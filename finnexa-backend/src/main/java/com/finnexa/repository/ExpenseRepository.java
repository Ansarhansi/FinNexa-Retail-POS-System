package com.finnexa.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.finnexa.entity.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	List<Expense> findByCategory(Expense.ExpenseCategory category);

	List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

	List<Expense> findByTitleContainingIgnoreCase(String title);

	List<Expense> findByPaymentMethod(Expense.PaymentMethod paymentMethod);

}