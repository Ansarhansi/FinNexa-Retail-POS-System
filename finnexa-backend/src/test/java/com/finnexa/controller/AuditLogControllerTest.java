package com.finnexa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finnexa.dto.AuditLogRequest;
import com.finnexa.dto.AuditLogResponse;
import com.finnexa.service.AuditLogService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private AuditLogService auditLogService;

	@Test
	void getAllAuditLogs_ShouldReturnOk() throws Exception {
		when(auditLogService.getAllAuditLogs()).thenReturn(Collections.emptyList());

		mockMvc.perform(get("/api/audit-logs").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getAuditLogsByUsername_ShouldReturnOk() throws Exception {
		when(auditLogService.getAuditLogsByUsername("admin")).thenReturn(Collections.emptyList());

		mockMvc.perform(get("/api/audit-logs/user/admin").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void getAuditLogsByAction_ShouldReturnOk() throws Exception {
		AuditLogResponse response = new AuditLogResponse(1L, "admin", "CREATE", "Product", 10L, "Product created",
				java.time.LocalDateTime.of(2026, 1, 15, 10, 30));

		when(auditLogService.getAuditLogsByAction("CREATE")).thenReturn(java.util.Collections.singletonList(response));

		mockMvc.perform(get("/api/audit-logs/action").param("action", "CREATE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].action").value("CREATE"));

	}

	@Test
	void getAuditLogsByAction_ShouldReturnBadRequest_WhenActionIsMissing() throws Exception {
		mockMvc.perform(get("/api/audit-logs/action").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createAuditLog_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
		AuditLogRequest request = new AuditLogRequest();
		request.setAction("CREATE");
		request.setEntityType("Product");
		request.setEntityId(10L);
		request.setDetails("Product created");

		AuditLogResponse response = new AuditLogResponse(1L, "admin", "CREATE", "Product", 10L, "Product created",
				LocalDateTime.of(2026, 1, 15, 10, 30));

		when(auditLogService.createAuditLog(any(AuditLogRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/audit-logs").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.action").value("CREATE")).andExpect(jsonPath("$.entityType").value("Product"))
				.andExpect(jsonPath("$.entityId").value(10)).andExpect(jsonPath("$.details").value("Product created"));
	}

}
