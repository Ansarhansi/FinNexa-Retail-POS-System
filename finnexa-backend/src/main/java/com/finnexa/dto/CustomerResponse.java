package com.finnexa.dto;

import java.time.LocalDateTime;

import com.finnexa.entity.Customer;

public class CustomerResponse {

	private Long id;
	private String name;
	private String email;
	private String phone;
	private String address;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public CustomerResponse() {
	}

	public CustomerResponse(Customer customer) {
		this.id = customer.getId();
		this.name = customer.getName();
		this.email = customer.getEmail();
		this.phone = customer.getPhone();
		this.address = customer.getAddress();
		this.createdAt = customer.getCreatedAt();
		this.updatedAt = customer.getUpdatedAt();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getAddress() {
		return address;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}