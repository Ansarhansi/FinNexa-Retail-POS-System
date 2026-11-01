
package com.finnexa.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.UserResponse;
import com.finnexa.entity.Role;
import com.finnexa.entity.User;
import com.finnexa.exception.BadRequestException;
import com.finnexa.exception.ResourceNotFoundException;
import com.finnexa.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class UserManagementService {

	private final UserRepository userRepository;
	private final AuditLogService auditLogService;

	public UserManagementService(UserRepository userRepository, AuditLogService auditLogService) {

		this.userRepository = userRepository;
		this.auditLogService = auditLogService;
	}

	// Retrieve all users
	public List<UserResponse> getAllUsers() {

		return userRepository.findAll().stream().map(this::mapToResponse).toList();
	}

	// Retrieve a user by ID
	public UserResponse getUserById(Long userId) {

		return mapToResponse(findUser(userId));
	}

	// Update a user's role
	@Transactional
	public UserResponse updateUserRole(Long userId, Role role) {

		if (role == null) {
			throw new BadRequestException("Role is required.");
		}

		User user = findUser(userId);

		Role previousRole = user.getRole();

		if (previousRole == role) {
			throw new BadRequestException("User already has the role: " + role);
		}

		user.setRole(role);

		User updatedUser = userRepository.save(user);

		auditLogService.recordAction("USER_ROLE_UPDATED", "User", updatedUser.getId(), "Role changed for user: "
				+ updatedUser.getEmail() + "; Previous role: " + previousRole + "; New role: " + updatedUser.getRole());

		return mapToResponse(updatedUser);
	}

	// Enable or disable a user account
	@Transactional
	public UserResponse updateUserStatus(Long userId, Boolean enabled) {

		if (enabled == null) {
			throw new BadRequestException("Enabled status is required.");
		}

		User user = findUser(userId);

		boolean previousStatus = user.isEnabled();

		if (previousStatus == enabled) {
			throw new BadRequestException("User account is already " + (enabled ? "enabled." : "disabled."));
		}

		user.setEnabled(enabled);

		User updatedUser = userRepository.save(user);

		auditLogService.recordAction("USER_STATUS_UPDATED", "User", updatedUser.getId(),
				"Account status changed for user: " + updatedUser.getEmail() + "; Previous enabled status: "
						+ previousStatus + "; New enabled status: " + updatedUser.isEnabled());

		return mapToResponse(updatedUser);
	}

	// Find a user by ID
	private User findUser(Long userId) {

		if (userId == null || userId <= 0) {
			throw new BadRequestException("User ID must be a positive number.");
		}

		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
	}

	// Map user entity to response DTO
	private UserResponse mapToResponse(User user) {

		return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
				user.isEnabled(), user.getCreatedAt());
	}
}
