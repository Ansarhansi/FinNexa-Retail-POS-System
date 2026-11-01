package com.finnexa.dto;

import java.time.LocalDateTime;

import com.finnexa.entity.Supplier;

public class SupplierResponse {

	private Long id;
	private String name;
	private String contactPerson;
	private String email;
	private String phone;
	private String address;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public SupplierResponse() {
	}

	public SupplierResponse(Supplier supplier) {
		this.id = supplier.getId();
		this.name = supplier.getName();
		this.contactPerson = supplier.getContactPerson();
		this.email = supplier.getEmail();
		this.phone = supplier.getPhone();
		this.address = supplier.getAddress();
		this.createdAt = supplier.getCreatedAt();
		this.updatedAt = supplier.getUpdatedAt();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getContactPerson() {
		return contactPerson;
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