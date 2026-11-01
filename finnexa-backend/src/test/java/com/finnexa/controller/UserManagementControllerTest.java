
package com.finnexa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finnexa.dto.UpdateUserRoleRequest;
import com.finnexa.dto.UpdateUserStatusRequest;
import com.finnexa.dto.UserResponse;
import com.finnexa.entity.Role;
import com.finnexa.service.UserManagementService;

@WebMvcTest(UserManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserManagementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private UserManagementService userManagementService;

	private UserResponse userResponse;

	@BeforeEach
	void setUp() {
		userResponse = new UserResponse(1L, "Test Admin", "admin@example.com", "ADMIN", true,
				LocalDateTime.of(2026, 1, 15, 10, 30));
	}

	@Test
	void getAllUsers_ShouldReturnOk() throws Exception {
		when(userManagementService.getAllUsers()).thenReturn(List.of(userResponse));

		mockMvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].fullName").value("Test Admin"))
				.andExpect(jsonPath("$[0].email").value("admin@example.com"))
				.andExpect(jsonPath("$[0].role").value("ADMIN")).andExpect(jsonPath("$[0].enabled").value(true));

		verify(userManagementService).getAllUsers();
	}

	@Test
	void getUserById_ShouldReturnOk() throws Exception {
		when(userManagementService.getUserById(1L)).thenReturn(userResponse);

		mockMvc.perform(get("/api/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.fullName").value("Test Admin"))
				.andExpect(jsonPath("$.email").value("admin@example.com"));

		verify(userManagementService).getUserById(1L);
	}

	@Test
	void updateUserRole_ShouldReturnOk() throws Exception {
		UpdateUserRoleRequest request = new UpdateUserRoleRequest();
		request.setRole(Role.ADMIN);

		when(userManagementService.updateUserRole(eq(1L), eq(Role.ADMIN))).thenReturn(userResponse);

		mockMvc.perform(patch("/api/users/1/role").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.role").value("ADMIN"));

		verify(userManagementService).updateUserRole(1L, Role.ADMIN);
	}

	@Test
	void updateUserStatus_ShouldReturnOk() throws Exception {
		UpdateUserStatusRequest request = new UpdateUserStatusRequest();
		request.setEnabled(false);

		UserResponse disabledUser = new UserResponse(1L, "Test Admin", "admin@example.com", "ADMIN", false,
				LocalDateTime.of(2026, 1, 15, 10, 30));

		when(userManagementService.updateUserStatus(1L, false)).thenReturn(disabledUser);

		mockMvc.perform(patch("/api/users/1/status").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.enabled").value(false));

		verify(userManagementService).updateUserStatus(1L, false);
	}
}
