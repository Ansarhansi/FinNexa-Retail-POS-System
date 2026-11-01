
package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.finnexa.dto.AuditLogRequest;
import com.finnexa.dto.AuditLogResponse;
import com.finnexa.entity.AuditLog;
import com.finnexa.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

	@Mock
	private AuditLogRepository auditLogRepository;

	@InjectMocks
	private AuditLogService auditLogService;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// --------------------------------------------------
	// createAuditLog() tests
	// --------------------------------------------------

	@Test
	void createAuditLog_shouldSaveAuditLog() {
		AuditLogRequest request = createRequest(" PRODUCT_CREATED ", "Product", 1L, "Product created successfully");

		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuditLogResponse response = auditLogService.createAuditLog(request);

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

		verify(auditLogRepository).save(captor.capture());

		AuditLog savedLog = captor.getValue();

		assertEquals("SYSTEM", savedLog.getUsername());
		assertEquals("PRODUCT_CREATED", savedLog.getAction());
		assertEquals("Product", savedLog.getEntityType());
		assertEquals(1L, savedLog.getEntityId());
		assertEquals("Product created successfully", savedLog.getDetails());

		assertEquals("SYSTEM", response.getUsername());
		assertEquals("PRODUCT_CREATED", response.getAction());
		assertEquals("Product", response.getEntityType());
		assertEquals(1L, response.getEntityId());
		assertEquals("Product created successfully", response.getDetails());

		// ID and createdAt are managed by JPA.
		assertNull(response.getId());
		assertNull(response.getCreatedAt());
	}

	@Test
	void createAuditLog_shouldThrowExceptionWhenRequestIsNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> auditLogService.createAuditLog(null));

		assertEquals("Action is required.", exception.getMessage());

		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void createAuditLog_shouldThrowExceptionWhenActionIsNull() {
		AuditLogRequest request = createRequest(null, "Product", 1L, "Test");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> auditLogService.createAuditLog(request));

		assertEquals("Action is required.", exception.getMessage());

		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void createAuditLog_shouldThrowExceptionWhenActionIsBlank() {
		AuditLogRequest request = createRequest("   ", "Product", 1L, "Test");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> auditLogService.createAuditLog(request));

		assertEquals("Action is required.", exception.getMessage());

		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	// --------------------------------------------------
	// recordAction() tests
	// --------------------------------------------------

	@Test
	void recordAction_shouldUseAuthenticatedUsername() {
		SecurityContextHolder.getContext()
				.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("ansar", "password", List.of()));

		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuditLogResponse response = auditLogService.recordAction(" SALE_CREATED ", "Sale", 10L, "Sale completed");

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

		verify(auditLogRepository).save(captor.capture());

		AuditLog savedLog = captor.getValue();

		assertEquals("ansar", savedLog.getUsername());
		assertEquals("SALE_CREATED", savedLog.getAction());
		assertEquals("Sale", savedLog.getEntityType());
		assertEquals(10L, savedLog.getEntityId());
		assertEquals("Sale completed", savedLog.getDetails());

		assertEquals("ansar", response.getUsername());
		assertEquals("SALE_CREATED", response.getAction());
	}

	@Test
	void recordAction_shouldUseSystemWhenAuthenticationIsMissing() {
		SecurityContextHolder.clearContext();

		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuditLogResponse response = auditLogService.recordAction("SYSTEM_ACTION", "System", 1L, "System operation");

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

		verify(auditLogRepository).save(captor.capture());

		assertEquals("SYSTEM", captor.getValue().getUsername());
		assertEquals("SYSTEM", response.getUsername());
	}

	@Test
	void recordAction_shouldUseSystemWhenAuthenticationIsAnonymous() {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("anonymousUser", "password"));

		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuditLogResponse response = auditLogService.recordAction("ANONYMOUS_ACTION", "System", 1L,
				"Anonymous operation");

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

		verify(auditLogRepository).save(captor.capture());

		assertEquals("SYSTEM", captor.getValue().getUsername());
		assertEquals("SYSTEM", response.getUsername());
	}

	@Test
	void recordAction_shouldThrowExceptionWhenActionIsNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> auditLogService.recordAction(null, "Product", 1L, "Test"));

		assertEquals("Audit action is required.", exception.getMessage());

		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void recordAction_shouldThrowExceptionWhenActionIsBlank() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> auditLogService.recordAction("   ", "Product", 1L, "Test"));

		assertEquals("Audit action is required.", exception.getMessage());

		verify(auditLogRepository, never()).save(any(AuditLog.class));
	}

	@Test
	void recordAction_shouldPreserveNullOptionalFields() {
		when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuditLogResponse response = auditLogService.recordAction("GENERAL_ACTION", null, null, null);

		assertEquals("GENERAL_ACTION", response.getAction());
		assertNull(response.getEntityType());
		assertNull(response.getEntityId());
		assertNull(response.getDetails());
	}

	// --------------------------------------------------
	// getAllAuditLogs() tests
	// --------------------------------------------------

	@Test
	void getAllAuditLogs_shouldReturnAllAuditLogs() {
		AuditLog log1 = createAuditLog("admin", "PRODUCT_CREATED", "Product", 1L, "Created product");

		AuditLog log2 = createAuditLog("cashier", "SALE_CREATED", "Sale", 2L, "Created sale");

		when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(log1, log2));

		List<AuditLogResponse> responses = auditLogService.getAllAuditLogs();

		assertEquals(2, responses.size());
		assertEquals("PRODUCT_CREATED", responses.get(0).getAction());
		assertEquals("SALE_CREATED", responses.get(1).getAction());
		assertEquals("admin", responses.get(0).getUsername());
		assertEquals("cashier", responses.get(1).getUsername());
	}

	@Test
	void getAllAuditLogs_shouldReturnEmptyListWhenNoLogsExist() {
		when(auditLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

		List<AuditLogResponse> responses = auditLogService.getAllAuditLogs();

		assertNotNull(responses);
		assertEquals(0, responses.size());
	}

	// --------------------------------------------------
	// getAuditLogsByUsername() tests
	// --------------------------------------------------

	@Test
	void getAuditLogsByUsername_shouldReturnMatchingLogs() {
		AuditLog log = createAuditLog("admin", "USER_UPDATED", "User", 5L, "User updated");

		when(auditLogRepository.findByUsernameOrderByCreatedAtDesc("admin")).thenReturn(List.of(log));

		List<AuditLogResponse> responses = auditLogService.getAuditLogsByUsername("admin");

		assertEquals(1, responses.size());
		assertEquals("admin", responses.get(0).getUsername());
		assertEquals("USER_UPDATED", responses.get(0).getAction());

		verify(auditLogRepository).findByUsernameOrderByCreatedAtDesc("admin");
	}

	@Test
	void getAuditLogsByUsername_shouldReturnEmptyListWhenNoMatchesExist() {
		when(auditLogRepository.findByUsernameOrderByCreatedAtDesc("unknown")).thenReturn(List.of());

		List<AuditLogResponse> responses = auditLogService.getAuditLogsByUsername("unknown");

		assertEquals(0, responses.size());
	}

	// --------------------------------------------------
	// getAuditLogsByAction() tests
	// --------------------------------------------------

	@Test
	void getAuditLogsByAction_shouldReturnMatchingLogs() {
		AuditLog log = createAuditLog("admin", "PRODUCT_CREATED", "Product", 5L, "Product created");

		when(auditLogRepository.findByActionOrderByCreatedAtDesc("PRODUCT_CREATED")).thenReturn(List.of(log));

		List<AuditLogResponse> responses = auditLogService.getAuditLogsByAction("PRODUCT_CREATED");

		assertEquals(1, responses.size());
		assertEquals("PRODUCT_CREATED", responses.get(0).getAction());
		assertEquals("Product", responses.get(0).getEntityType());

		verify(auditLogRepository).findByActionOrderByCreatedAtDesc("PRODUCT_CREATED");
	}

	@Test
	void getAuditLogsByAction_shouldReturnEmptyListWhenNoMatchesExist() {
		when(auditLogRepository.findByActionOrderByCreatedAtDesc("UNKNOWN")).thenReturn(List.of());

		List<AuditLogResponse> responses = auditLogService.getAuditLogsByAction("UNKNOWN");

		assertEquals(0, responses.size());
	}

	// --------------------------------------------------
	// Helper methods
	// --------------------------------------------------

	private AuditLogRequest createRequest(String action, String entityType, Long entityId, String details) {

		AuditLogRequest request = new AuditLogRequest();
		request.setAction(action);
		request.setEntityType(entityType);
		request.setEntityId(entityId);
		request.setDetails(details);

		return request;
	}

	private AuditLog createAuditLog(String username, String action, String entityType, Long entityId, String details) {

		AuditLog auditLog = new AuditLog();
		auditLog.setUsername(username);
		auditLog.setAction(action);
		auditLog.setEntityType(entityType);
		auditLog.setEntityId(entityId);
		auditLog.setDetails(details);

		// Simulate the JPA lifecycle callback.
		auditLog.prePersist();

		return auditLog;
	}
}
