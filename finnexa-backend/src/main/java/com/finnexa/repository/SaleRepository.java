package com.finnexa.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.finnexa.entity.Sale;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

	Optional<Sale> findByInvoiceNumber(String invoiceNumber);

	List<Sale> findByStatus(Sale.SaleStatus status);

	List<Sale> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);

	boolean existsByInvoiceNumber(String invoiceNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT s FROM Sale s WHERE s.id = :saleId")
	Optional<Sale> findByIdForUpdate(@Param("saleId") Long saleId);

	List<Sale> findByStatusAndSaleDateBetween(Sale.SaleStatus status, LocalDateTime startDate, LocalDateTime endDate);
}