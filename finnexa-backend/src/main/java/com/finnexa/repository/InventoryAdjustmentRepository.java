package com.finnexa.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.finnexa.entity.InventoryAdjustment;

@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
	List<InventoryAdjustment> findByProductIdOrderByAdjustedAtDesc(Long productId);

	List<InventoryAdjustment> findAllByOrderByAdjustedAtDesc();
}