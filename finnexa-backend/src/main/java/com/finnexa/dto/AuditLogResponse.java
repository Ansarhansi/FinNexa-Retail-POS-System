package com.finnexa.dto;

import java.time.LocalDateTime;

public class AuditLogResponse {
	private Long id;
	private String username;
	private String action;
	private String entityType;
	private Long entityId;
	private String details;
	private LocalDateTime createdAt;

	public AuditLogResponse() {
	}

	public AuditLogResponse(Long id, String username, String action, String entityType, Long entityId, String details,
			LocalDateTime createdAt) {
		this.id = id;
		this.username = username;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.details = details;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getAction() {
		return action;
	}

	public String getEntityType() {
		return entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public String getDetails() {
		return details;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}