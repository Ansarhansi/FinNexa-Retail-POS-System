package com.finnexa.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.finnexa.dto.AuditLogRequest;
import com.finnexa.dto.AuditLogResponse;
import com.finnexa.service.AuditLogService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs() {
		return ResponseEntity.ok(auditLogService.getAllAuditLogs());
	}

	@GetMapping("/user/{username}")
	public ResponseEntity<List<AuditLogResponse>> getAuditLogsByUsername(@PathVariable String username) {
		return ResponseEntity.ok(auditLogService.getAuditLogsByUsername(username));
	}

	@GetMapping("/action")
	public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAction(@RequestParam String action) {
		return ResponseEntity.ok(auditLogService.getAuditLogsByAction(action));
	}

	@PostMapping
	public ResponseEntity<AuditLogResponse> createAuditLog(@Valid @RequestBody AuditLogRequest request) {
		AuditLogResponse response = auditLogService.createAuditLog(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}