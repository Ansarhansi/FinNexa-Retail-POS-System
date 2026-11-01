package com.finnexa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuditLogRequest {
	@NotBlank(message = "Action is required")
	@Size(max = 100, message = "Action cannot exceed 100 characters")
	private String action;
	@Size(max = 100, message = "Entity type cannot exceed 100 characters")
	private String entityType;
	private Long entityId;
	@Size(max = 1000, message = "Details cannot exceed 1000 characters")
	private String details;

	public AuditLogRequest() {
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}
}