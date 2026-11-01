package com.finnexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.finnexa.entity.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

	Optional<Supplier> findByEmail(String email);

	List<Supplier> findByNameContainingIgnoreCase(String name);

	List<Supplier> findByContactPersonContainingIgnoreCase(String contactPerson);

	List<Supplier> findByPhone(String phone);

	boolean existsByEmail(String email);

}