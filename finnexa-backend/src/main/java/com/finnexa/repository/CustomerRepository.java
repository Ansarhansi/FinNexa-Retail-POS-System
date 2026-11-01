
package com.finnexa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.finnexa.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByEmail(String email);

	List<Customer> findByNameContainingIgnoreCase(String name);

	boolean existsByEmail(String email);

	List<Customer> findByPhone(String phone);

}