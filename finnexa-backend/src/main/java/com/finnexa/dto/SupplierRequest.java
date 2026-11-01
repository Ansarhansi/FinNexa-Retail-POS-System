package com.finnexa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierRequest {

	@NotBlank(message = "Supplier name is required")
	@Size(max = 100, message = "Supplier name cannot exceed 100 characters")
	private String name;

	@NotBlank(message = "Contact person is required")
	@Size(max = 100, message = "Contact person cannot exceed 100 characters")
	private String contactPerson;

	@Email(message = "Enter a valid email address")
	@Size(max = 150, message = "Email cannot exceed 150 characters")
	private String email;

	@Size(max = 20, message = "Phone cannot exceed 20 characters")
	private String phone;

	@Size(max = 250, message = "Address cannot exceed 250 characters")
	private String address;

	public SupplierRequest() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContactPerson() {
		return contactPerson;
	}

	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

}