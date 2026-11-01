package com.finnexa.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finnexa.dto.UpdateUserRoleRequest;
import com.finnexa.dto.UpdateUserStatusRequest;
import com.finnexa.dto.UserResponse;
import com.finnexa.service.UserManagementService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {
	private final UserManagementService userManagementService;

	public UserManagementController(UserManagementService userManagementService) {
		this.userManagementService = userManagementService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		return ResponseEntity.ok(userManagementService.getAllUsers());
	}

	@GetMapping("/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
		return ResponseEntity.ok(userManagementService.getUserById(userId));
	}

	@PatchMapping("/{userId}/role")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long userId,
			@Valid @RequestBody UpdateUserRoleRequest request) {
		return ResponseEntity.ok(userManagementService.updateUserRole(userId, request.getRole()));
	}

	@PatchMapping("/{userId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long userId,
			@Valid @RequestBody UpdateUserStatusRequest request) {
		return ResponseEntity.ok(userManagementService.updateUserStatus(userId, request.getEnabled()));
	}
}