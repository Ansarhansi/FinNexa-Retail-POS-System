package com.finnexa.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.finnexa.dto.UpdateProfileRequest;
import com.finnexa.dto.ChangePasswordRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.finnexa.dto.AuthResponse;
import com.finnexa.dto.LoginRequest;
import com.finnexa.dto.RegisterRequest;
import com.finnexa.exception.BadRequestException;
import com.finnexa.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		try {
			AuthResponse response = authService.login(request);
			return ResponseEntity.ok(response);
		} catch (BadCredentialsException exception) {
			throw new BadRequestException("Invalid email or password.");
		} catch (DisabledException exception) {
			throw new BadRequestException("This user account is disabled.");
		}
	}

	// Update logged-in user's profile
	@PatchMapping("/profile")
	public ResponseEntity<AuthResponse> updateProfile(Authentication authentication,
			@Valid @RequestBody UpdateProfileRequest request) {

		AuthResponse response = authService.updateProfile(authentication.getName(), request);

		return ResponseEntity.ok(response);
	}

	// Change logged-in user's password
	@PatchMapping("/change-password")
	public ResponseEntity<String> changePassword(Authentication authentication,
			@Valid @RequestBody ChangePasswordRequest request) {

		authService.changePassword(authentication.getName(), request);

		return ResponseEntity.ok("Password changed successfully.");
	}
}