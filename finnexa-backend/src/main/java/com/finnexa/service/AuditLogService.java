package com.finnexa.service;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finnexa.dto.AuditLogRequest;
import com.finnexa.dto.AuditLogResponse;
import com.finnexa.entity.AuditLog;
import com.finnexa.repository.AuditLogRepository;

@Service
public class AuditLogService {
	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	/** * Creates an audit record from an incoming request. */
	@Transactional
	public AuditLogResponse createAuditLog(AuditLogRequest request) {
		if (request == null || request.getAction() == null || request.getAction().isBlank()) {
			throw new IllegalArgumentException("Action is required.");
		}
		return recordAction(request.getAction(), request.getEntityType(), request.getEntityId(), request.getDetails());
	}

	/**
	 * * Internal method for recording business operations. * Business services can
	 * call this method directly.
	 */
	@Transactional
	public AuditLogResponse recordAction(String action, String entityType, Long entityId, String details) {
		if (action == null || action.isBlank()) {
			throw new IllegalArgumentException("Audit action is required.");
		}
		AuditLog auditLog = new AuditLog();
		auditLog.setUsername(getCurrentUsername());
		auditLog.setAction(action.trim());
		auditLog.setEntityType(entityType);
		auditLog.setEntityId(entityId);
		auditLog.setDetails(details);
		AuditLog savedLog = auditLogRepository.save(auditLog);
		return mapToResponse(savedLog);
	}

	@Transactional(readOnly = true)
	public List<AuditLogResponse> getAllAuditLogs() {
		return auditLogRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapToResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<AuditLogResponse> getAuditLogsByUsername(String username) {
		return auditLogRepository.findByUsernameOrderByCreatedAtDesc(username).stream().map(this::mapToResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AuditLogResponse> getAuditLogsByAction(String action) {
		return auditLogRepository.findByActionOrderByCreatedAtDesc(action).stream().map(this::mapToResponse).toList();
	}

	private String getCurrentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getName())) {
			return "SYSTEM";
		}
		return authentication.getName();
	}

	private AuditLogResponse mapToResponse(AuditLog auditLog) {
		return new AuditLogResponse(auditLog.getId(), auditLog.getUsername(), auditLog.getAction(),
				auditLog.getEntityType(), auditLog.getEntityId(), auditLog.getDetails(), auditLog.getCreatedAt());
	}
}