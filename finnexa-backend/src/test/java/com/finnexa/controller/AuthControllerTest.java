package com.finnexa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finnexa.dto.AuthResponse;
import com.finnexa.dto.LoginRequest;
import com.finnexa.dto.RegisterRequest;
import com.finnexa.exception.BadRequestException;
import com.finnexa.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private AuthService authService;

	@Test
	void register_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFullName("John Doe");
		request.setEmail("john@example.com");
		request.setPassword("Password123");

		AuthResponse response = new AuthResponse(1L, "John Doe", "john@example.com", "USER", "Registration successful");

		when(authService.register(any(RegisterRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.fullName").value("John Doe"))
				.andExpect(jsonPath("$.email").value("john@example.com")).andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.message").value("Registration successful"));
	}

	@Test
	void register_ShouldReturnBadRequest_WhenFullNameIsBlank() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFullName("");
		request.setEmail("john@example.com");
		request.setPassword("Password123");

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void register_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFullName("John Doe");
		request.setEmail("invalid-email");
		request.setPassword("Password123");

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void register_ShouldReturnBadRequest_WhenPasswordIsTooShort() throws Exception {
		RegisterRequest request = new RegisterRequest();
		request.setFullName("John Doe");
		request.setEmail("john@example.com");
		request.setPassword("short");

		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void login_ShouldReturnOk_WhenCredentialsAreValid() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("Password123");

		AuthResponse response = new AuthResponse(1L, "John Doe", "john@example.com", "USER", "Login successful");

		when(authService.login(any(LoginRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.fullName").value("John Doe"))
				.andExpect(jsonPath("$.email").value("john@example.com")).andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.message").value("Login successful"));
	}

	@Test
	void login_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("invalid-email");
		request.setPassword("Password123");

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void login_ShouldReturnBadRequest_WhenPasswordIsBlank() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("");

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void login_ShouldReturnBadRequest_WhenCredentialsAreIncorrect() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("WrongPassword123");

		when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	@Test
	void login_ShouldReturnBadRequest_WhenAccountIsDisabled() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setEmail("john@example.com");
		request.setPassword("Password123");

		when(authService.login(any(LoginRequest.class))).thenThrow(new DisabledException("Account disabled"));

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

}
