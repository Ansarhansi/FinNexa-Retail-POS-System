package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.finnexa.dto.AuthResponse;
import com.finnexa.dto.LoginRequest;
import com.finnexa.dto.RegisterRequest;
import com.finnexa.entity.Role;
import com.finnexa.entity.User;
import com.finnexa.exception.BadRequestException;
import com.finnexa.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private Authentication authentication;

	@InjectMocks
	private AuthService authService;

	private RegisterRequest registerRequest;
	private LoginRequest loginRequest;
	private User user;

	@BeforeEach
	void setUp() {
		registerRequest = new RegisterRequest();
		registerRequest.setFullName("Test User");
		registerRequest.setEmail("test@example.com");
		registerRequest.setPassword("Password123!");

		loginRequest = new LoginRequest();
		loginRequest.setEmail("test@example.com");
		loginRequest.setPassword("Password123!");

		user = createUser(1L, "Test User", "test@example.com", "encoded-password", Role.CASHIER, true);
	}

// =========================================================
// HELPER METHOD
// =========================================================

	private User createUser(Long id, String fullName, String email, String password, Role role, boolean enabled) {

		User testUser = new User();

		// The User entity does not expose setId().
		// Assign the generated ID for unit testing only.
		try {
			Field idField = User.class.getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(testUser, id);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException("Unable to assign the test user ID", e);
		}

		testUser.setFullName(fullName);
		testUser.setEmail(email);
		testUser.setPassword(password);
		testUser.setRole(role);
		testUser.setEnabled(enabled);

		return testUser;
	}

// =========================================================
// 1. REGISTRATION SUCCESS TESTS
// =========================================================

	@Test
	void registerShouldRegisterUserSuccessfully() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

		AuthResponse response = authService.register(registerRequest);

		assertNotNull(response);
		assertEquals(1L, response.getUserId());
		assertEquals("Test User", response.getFullName());
		assertEquals("test@example.com", response.getEmail());
		assertEquals("CASHIER", response.getRole());
		assertEquals("User registered successfully.", response.getMessage());

		verify(userRepository).existsByEmailIgnoreCase("test@example.com");
		verify(passwordEncoder).encode("Password123!");
		verify(userRepository).saveAndFlush(any(User.class));
	}

	@Test
	void registerShouldNormalizeEmailToLowercase() {
		registerRequest.setEmail("  TEST@EXAMPLE.COM  ");

		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

		AuthResponse response = authService.register(registerRequest);

		assertEquals("test@example.com", response.getEmail());

		verify(userRepository).existsByEmailIgnoreCase("test@example.com");
	}

	@Test
	void registerShouldTrimFullName() {
		registerRequest.setFullName("  Test User  ");

		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			return createUser(1L, savedUser.getFullName(), savedUser.getEmail(), savedUser.getPassword(),
					savedUser.getRole(), savedUser.isEnabled());
		});

		AuthResponse response = authService.register(registerRequest);

		assertEquals("Test User", response.getFullName());
	}

	@Test
	void registerShouldAssignCashierRole() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			return createUser(1L, savedUser.getFullName(), savedUser.getEmail(), savedUser.getPassword(),
					savedUser.getRole(), savedUser.isEnabled());
		});

		authService.register(registerRequest);

		verify(userRepository).saveAndFlush(argThat(savedUser -> savedUser.getRole() == Role.CASHIER));
	}

	@Test
	void registerShouldEnableNewAccount() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			return createUser(1L, savedUser.getFullName(), savedUser.getEmail(), savedUser.getPassword(),
					savedUser.getRole(), savedUser.isEnabled());
		});

		authService.register(registerRequest);

		verify(userRepository).saveAndFlush(argThat(User::isEnabled));
	}

	@Test
	void registerShouldEncodePasswordBeforeSaving() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);
			assertEquals("encoded-password", savedUser.getPassword());
			return savedUser;
		});

		authService.register(registerRequest);

		verify(passwordEncoder).encode("Password123!");
	}

	@Test
	void registerShouldSaveNormalizedUserDetails() {
		registerRequest.setFullName("  Test User  ");
		registerRequest.setEmail("  TEST@EXAMPLE.COM  ");

		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
			User savedUser = invocation.getArgument(0);

			assertEquals("Test User", savedUser.getFullName());
			assertEquals("test@example.com", savedUser.getEmail());
			assertEquals("encoded-password", savedUser.getPassword());
			assertEquals(Role.CASHIER, savedUser.getRole());
			assertTrue(savedUser.isEnabled());

			return createUser(1L, savedUser.getFullName(), savedUser.getEmail(), savedUser.getPassword(),
					savedUser.getRole(), savedUser.isEnabled());
		});

		authService.register(registerRequest);
	}

// =========================================================
// 2. REGISTRATION VALIDATION TESTS
// =========================================================

	@Test
	void registerShouldRejectNullRequest() {
		assertThrows(BadRequestException.class, () -> authService.register(null));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void registerShouldRejectNullFullName() {
		registerRequest.setFullName(null);

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void registerShouldRejectBlankFullName() {
		registerRequest.setFullName("   ");

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void registerShouldRejectNullEmail() {
		registerRequest.setEmail(null);

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void registerShouldRejectBlankEmail() {
		registerRequest.setEmail("   ");

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void registerShouldRejectNullPassword() {
		registerRequest.setPassword(null);

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void registerShouldRejectBlankPassword() {
		registerRequest.setPassword("   ");

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void registerShouldRejectExistingEmail() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verify(userRepository, never()).saveAndFlush(any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void registerShouldNotSaveWhenEmailAlreadyExists() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

	@Test
	void registerShouldHandleDatabaseEmailConstraintViolation() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class)))
				.thenThrow(new DataIntegrityViolationException("Duplicate email"));

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
	}

	@Test
	void registerShouldNotSaveWhenRequestIsInvalid() {
		registerRequest.setEmail(null);

		assertThrows(BadRequestException.class, () -> authService.register(registerRequest));

		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

// =========================================================
// 3. LOGIN SUCCESS TESTS
// =========================================================

	@Test
	void loginShouldAuthenticateSuccessfully() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		AuthResponse response = authService.login(loginRequest);

		assertNotNull(response);
		assertEquals(1L, response.getUserId());
		assertEquals("Test User", response.getFullName());
		assertEquals("test@example.com", response.getEmail());
		assertEquals("CASHIER", response.getRole());
		assertEquals("Login successful.", response.getMessage());

		verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
	}

	@Test
	void loginShouldNormalizeEmailToLowercase() {
		loginRequest.setEmail("  TEST@EXAMPLE.COM  ");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		AuthResponse response = authService.login(loginRequest);

		assertEquals("test@example.com", response.getEmail());
	}

	@Test
	void loginShouldPassNormalizedEmailToAuthenticationManager() {
		loginRequest.setEmail("  TEST@EXAMPLE.COM  ");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		authService.login(loginRequest);

		verify(authenticationManager).authenticate(argThat(token -> "test@example.com".equals(token.getPrincipal())
				&& "Password123!".equals(token.getCredentials())));
	}

	@Test
	void loginShouldUseSubmittedPasswordForAuthentication() {
		loginRequest.setPassword("DifferentPassword123!");

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		authService.login(loginRequest);

		verify(authenticationManager)
				.authenticate(argThat(token -> "DifferentPassword123!".equals(token.getCredentials())));
	}

	@Test
	void loginShouldRetrieveUserUsingAuthenticatedEmail() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		authService.login(loginRequest);

		verify(userRepository).findByEmailIgnoreCase("test@example.com");
	}

	@Test
	void loginShouldReturnAuthenticatedUserRole() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		AuthResponse response = authService.login(loginRequest);

		assertEquals("CASHIER", response.getRole());
	}

// =========================================================
// 4. LOGIN VALIDATION TESTS
// =========================================================

	@Test
	void loginShouldRejectNullRequest() {
		assertThrows(BadRequestException.class, () -> authService.login(null));

		verifyNoInteractions(authenticationManager, userRepository);
	}

	@Test
	void loginShouldRejectNullEmail() {
		loginRequest.setEmail(null);

		assertThrows(BadRequestException.class, () -> authService.login(loginRequest));

		verifyNoInteractions(authenticationManager, userRepository);
	}

	@Test
	void loginShouldRejectBlankEmail() {
		loginRequest.setEmail("   ");

		assertThrows(BadRequestException.class, () -> authService.login(loginRequest));

		verifyNoInteractions(authenticationManager, userRepository);
	}

	@Test
	void loginShouldRejectNullPassword() {
		loginRequest.setPassword(null);

		assertThrows(BadRequestException.class, () -> authService.login(loginRequest));

		verifyNoInteractions(authenticationManager, userRepository);
	}

	@Test
	void loginShouldRejectBlankPassword() {
		loginRequest.setPassword("   ");

		assertThrows(BadRequestException.class, () -> authService.login(loginRequest));

		verifyNoInteractions(authenticationManager, userRepository);
	}

// =========================================================
// 5. LOGIN AUTHENTICATION FAILURE TESTS
// =========================================================

	@Test
	void loginShouldPropagateBadCredentialsException() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Invalid username or password"));

		assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));

		verify(userRepository, never()).findByEmailIgnoreCase(anyString());
	}

	@Test
	void loginShouldPropagateAuthenticationFailure() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new org.springframework.security.authentication.AuthenticationServiceException(
						"Authentication service unavailable"));

		assertThrows(org.springframework.security.authentication.AuthenticationServiceException.class,
				() -> authService.login(loginRequest));
	}

	@Test
	void loginShouldRejectAuthenticatedUserMissingFromRepository() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("missing@example.com");

		when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

		assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
	}

	@Test
	void loginShouldNotEncodePasswordDuringLogin() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		authService.login(loginRequest);

		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void loginShouldNotSaveUser() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		authService.login(loginRequest);

		verify(userRepository, never()).save(any(User.class));
		verify(userRepository, never()).saveAndFlush(any(User.class));
	}

// =========================================================
// 6. REGISTRATION AND LOGIN RESPONSE TESTS
// =========================================================

	@Test
	void registerResponseShouldContainSuccessMessage() {
		when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);

		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");

		when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

		AuthResponse response = authService.register(registerRequest);

		assertEquals("User registered successfully.", response.getMessage());
	}

	@Test
	void loginResponseShouldContainSuccessMessage() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(authentication);

		when(authentication.getName()).thenReturn("test@example.com");

		when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

		AuthResponse response = authService.login(loginRequest);

		assertEquals("Login successful.", response.getMessage());
	}

}
