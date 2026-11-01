package com.finnexa.dto;

import java.time.LocalDateTime;

public class UserResponse {
	private Long id;
	private String fullName;
	private String email;
	private String role;
	private boolean enabled;
	private LocalDateTime createdAt;

	public UserResponse() {
	}

	public UserResponse(Long id, String fullName, String email, String role, boolean enabled, LocalDateTime createdAt) {
		this.id = id;
		this.fullName = fullName;
		this.email = email;
		this.role = role;
		this.enabled = enabled;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}