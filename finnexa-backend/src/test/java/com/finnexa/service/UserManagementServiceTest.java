package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finnexa.dto.UserResponse;
import com.finnexa.entity.Role;
import com.finnexa.entity.User;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AuditLogService auditLogService;

	@InjectMocks
	private UserManagementService userManagementService;

	private User user;

	@BeforeEach
	void setUp() {
		user = createUser(1L, "Test User", "test@example.com", Role.ADMIN, true);
	}

// =========================================================
// HELPER METHODS
// =========================================================

	private User createUser(Long id, String fullName, String email, Role role, boolean enabled) {

		User testUser = new User();

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
		testUser.setPassword("test-password");
		testUser.setRole(role);
		testUser.setEnabled(enabled);

		return testUser;
	}

// =========================================================
// 1. GET ALL USERS
// =========================================================

	@Test
	void getAllUsersShouldReturnAllUsers() {
		User secondUser = createUser(2L, "Second User", "second@example.com", Role.INVENTORY_MANAGER, true);

		when(userRepository.findAll()).thenReturn(List.of(user, secondUser));

		List<UserResponse> responses = userManagementService.getAllUsers();

		assertEquals(2, responses.size());

		assertEquals(1L, responses.get(0).getId());
		assertEquals("Test User", responses.get(0).getFullName());
		assertEquals("test@example.com", responses.get(0).getEmail());
		assertEquals("ADMIN", responses.get(0).getRole());
		assertTrue(responses.get(0).isEnabled());

		assertEquals(2L, responses.get(1).getId());
		assertEquals("Second User", responses.get(1).getFullName());
		assertEquals("second@example.com", responses.get(1).getEmail());
		assertEquals("INVENTORY_MANAGER", responses.get(1).getRole());

		verify(userRepository).findAll();
	}

	@Test
	void getAllUsersShouldReturnEmptyListWhenNoUsersExist() {
		when(userRepository.findAll()).thenReturn(List.of());

		List<UserResponse> responses = userManagementService.getAllUsers();

		assertNotNull(responses);
		assertTrue(responses.isEmpty());
	}

	@Test
	void getAllUsersShouldMapEnabledStatus() {
		when(userRepository.findAll()).thenReturn(List.of(user));

		List<UserResponse> responses = userManagementService.getAllUsers();

		assertTrue(responses.get(0).isEnabled());
	}

	@Test
	void getAllUsersShouldMapDisabledStatus() {
		user.setEnabled(false);

		when(userRepository.findAll()).thenReturn(List.of(user));

		List<UserResponse> responses = userManagementService.getAllUsers();

		assertFalse(responses.get(0).isEnabled());
	}

	@Test
	void getAllUsersShouldMapDifferentRoles() {
		User secondUser = createUser(2L, "Inventory User", "inventory@example.com", Role.INVENTORY_MANAGER, true);

		when(userRepository.findAll()).thenReturn(List.of(user, secondUser));

		List<UserResponse> responses = userManagementService.getAllUsers();

		assertEquals("ADMIN", responses.get(0).getRole());
		assertEquals("INVENTORY_MANAGER", responses.get(1).getRole());
	}

// =========================================================
// 2. GET USER BY ID
// =========================================================

	@Test
	void getUserByIdShouldReturnUser() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		UserResponse response = userManagementService.getUserById(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Test User", response.getFullName());
		assertEquals("test@example.com", response.getEmail());
		assertEquals("ADMIN", response.getRole());
		assertTrue(response.isEnabled());
	}

	@Test
	void getUserByIdShouldThrowWhenUserDoesNotExist() {
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userManagementService.getUserById(999L));
	}

	@Test
	void getUserByIdShouldRejectNullId() {
		assertThrows(BadRequestException.class, () -> userManagementService.getUserById(null));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void getUserByIdShouldRejectZeroId() {
		assertThrows(BadRequestException.class, () -> userManagementService.getUserById(0L));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void getUserByIdShouldRejectNegativeId() {
		assertThrows(BadRequestException.class, () -> userManagementService.getUserById(-1L));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void getUserByIdShouldMapUserRole() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		UserResponse response = userManagementService.getUserById(1L);

		assertEquals("ADMIN", response.getRole());
	}

	@Test
	void getUserByIdShouldMapDisabledStatus() {
		user.setEnabled(false);

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		UserResponse response = userManagementService.getUserById(1L);

		assertFalse(response.isEnabled());
	}

// =========================================================
// 3. UPDATE USER ROLE
// =========================================================

	@Test
	void updateUserRoleShouldUpdateRoleSuccessfully() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userManagementService.updateUserRole(1L, Role.INVENTORY_MANAGER);

		assertEquals("INVENTORY_MANAGER", response.getRole());
		assertEquals(Role.INVENTORY_MANAGER, user.getRole());

		verify(userRepository).save(user);
	}

	@Test
	void updateUserRoleShouldRecordAuditLog() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserRole(1L, Role.INVENTORY_MANAGER);

		verify(auditLogService).recordAction(eq("USER_ROLE_UPDATED"), eq("User"), eq(1L),
				contains("Previous role: ADMIN"));
	}

	@Test
	void updateUserRoleAuditLogShouldContainNewRole() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserRole(1L, Role.INVENTORY_MANAGER);

		verify(auditLogService).recordAction(eq("USER_ROLE_UPDATED"), eq("User"), eq(1L),
				contains("New role: INVENTORY_MANAGER"));
	}

	@Test
	void updateUserRoleAuditLogShouldContainEmail() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserRole(1L, Role.INVENTORY_MANAGER);

		verify(auditLogService).recordAction(eq("USER_ROLE_UPDATED"), eq("User"), eq(1L), contains("test@example.com"));
	}

	@Test
	void updateUserRoleShouldPreserveOtherUserDetails() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userManagementService.updateUserRole(1L, Role.INVENTORY_MANAGER);

		assertEquals("Test User", response.getFullName());
		assertEquals("test@example.com", response.getEmail());
		assertTrue(response.isEnabled());
	}

	@Test
	void updateUserRoleShouldRejectNullRole() {
		assertThrows(BadRequestException.class, () -> userManagementService.updateUserRole(1L, null));

		verify(userRepository, never()).findById(any());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUserRoleShouldRejectSameRole() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		assertThrows(BadRequestException.class, () -> userManagementService.updateUserRole(1L, Role.ADMIN));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(auditLogService);
	}

	@Test
	void updateUserRoleShouldRejectNullUserId() {
		assertThrows(BadRequestException.class,
				() -> userManagementService.updateUserRole(null, Role.INVENTORY_MANAGER));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void updateUserRoleShouldRejectZeroUserId() {
		assertThrows(BadRequestException.class, () -> userManagementService.updateUserRole(0L, Role.INVENTORY_MANAGER));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void updateUserRoleShouldRejectNegativeUserId() {
		assertThrows(BadRequestException.class,
				() -> userManagementService.updateUserRole(-1L, Role.INVENTORY_MANAGER));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void updateUserRoleShouldThrowWhenUserDoesNotExist() {
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> userManagementService.updateUserRole(999L, Role.INVENTORY_MANAGER));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(auditLogService);
	}

// =========================================================
// 4. UPDATE USER STATUS
// =========================================================

	@Test
	void updateUserStatusShouldDisableAccountSuccessfully() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userManagementService.updateUserStatus(1L, false);

		assertFalse(response.isEnabled());
		assertFalse(user.isEnabled());

		verify(userRepository).save(user);
	}

	@Test
	void updateUserStatusShouldEnableAccountSuccessfully() {
		user.setEnabled(false);

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = userManagementService.updateUserStatus(1L, true);

		assertTrue(response.isEnabled());
		assertTrue(user.isEnabled());
	}

	@Test
	void updateUserStatusShouldRecordAuditLogWhenDisabling() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserStatus(1L, false);

		verify(auditLogService).recordAction(eq("USER_STATUS_UPDATED"), eq("User"), eq(1L),
				contains("Previous enabled status: true"));
	}

	@Test
	void updateUserStatusAuditLogShouldContainNewDisabledStatus() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserStatus(1L, false);

		verify(auditLogService).recordAction(eq("USER_STATUS_UPDATED"), eq("User"), eq(1L),
				contains("New enabled status: false"));
	}

	@Test
	void updateUserStatusShouldRecordAuditLogWhenEnabling() {
		user.setEnabled(false);

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserStatus(1L, true);

		verify(auditLogService).recordAction(eq("USER_STATUS_UPDATED"), eq("User"), eq(1L),
				contains("Previous enabled status: false"));
	}

	@Test
	void updateUserStatusAuditLogShouldContainNewEnabledStatus() {
		user.setEnabled(false);

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserStatus(1L, true);

		verify(auditLogService).recordAction(eq("USER_STATUS_UPDATED"), eq("User"), eq(1L),
				contains("New enabled status: true"));
	}

	@Test
	void updateUserStatusAuditLogShouldContainUserEmail() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserStatus(1L, false);

		verify(auditLogService).recordAction(eq("USER_STATUS_UPDATED"), eq("User"), eq(1L),
				contains("test@example.com"));
	}

	@Test
	void updateUserStatusShouldPreserveUserRole() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserStatus(1L, false);

		assertEquals(Role.ADMIN, user.getRole());
	}

	@Test
	void updateUserStatusShouldRejectNullEnabledStatus() {
		assertThrows(BadRequestException.class, () -> userManagementService.updateUserStatus(1L, null));

		verify(userRepository, never()).findById(any());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateUserStatusShouldRejectAlreadyEnabledAccount() {
		user.setEnabled(true);

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		assertThrows(BadRequestException.class, () -> userManagementService.updateUserStatus(1L, true));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(auditLogService);
	}

	@Test
	void updateUserStatusShouldRejectAlreadyDisabledAccount() {
		user.setEnabled(false);

		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		assertThrows(BadRequestException.class, () -> userManagementService.updateUserStatus(1L, false));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(auditLogService);
	}

	@Test
	void updateUserStatusShouldRejectNullUserId() {
		assertThrows(BadRequestException.class, () -> userManagementService.updateUserStatus(null, false));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void updateUserStatusShouldRejectZeroUserId() {
		assertThrows(BadRequestException.class, () -> userManagementService.updateUserStatus(0L, false));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void updateUserStatusShouldRejectNegativeUserId() {
		assertThrows(BadRequestException.class, () -> userManagementService.updateUserStatus(-1L, false));

		verify(userRepository, never()).findById(any());
	}

	@Test
	void updateUserStatusShouldThrowWhenUserDoesNotExist() {
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userManagementService.updateUserStatus(999L, false));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(auditLogService);
	}

// =========================================================
// 5. REPOSITORY AND AUDIT INTERACTION TESTS
// =========================================================

	@Test
	void updateUserRoleShouldSaveExactlyOnce() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserRole(1L, Role.INVENTORY_MANAGER);

		verify(userRepository, times(1)).save(user);
	}

	@Test
	void updateUserStatusShouldSaveExactlyOnce() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userManagementService.updateUserStatus(1L, false);

		verify(userRepository, times(1)).save(user);
	}

	@Test
	void updateUserRoleShouldNotSaveWhenUserIsMissing() {
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> userManagementService.updateUserRole(999L, Role.INVENTORY_MANAGER));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(auditLogService);
	}

	@Test
	void updateUserStatusShouldNotSaveWhenUserIsMissing() {
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userManagementService.updateUserStatus(999L, false));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(auditLogService);
	}

}
