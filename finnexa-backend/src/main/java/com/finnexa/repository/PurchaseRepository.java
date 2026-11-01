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

import com.finnexa.entity.Purchase;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

	Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

	boolean existsByPurchaseNumber(String purchaseNumber);

	List<Purchase> findByStatus(Purchase.PurchaseStatus status);

	List<Purchase> findByPurchaseDateBetween(LocalDateTime startDate, LocalDateTime endDate);

	List<Purchase> findBySupplierId(Long supplierId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Purchase p WHERE p.id = :purchaseId")
	Optional<Purchase> findByIdForUpdate(@Param("purchaseId") Long purchaseId);
}
