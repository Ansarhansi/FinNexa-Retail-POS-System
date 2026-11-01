package com.finnexa.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finnexa.dto.AuthResponse;
import com.finnexa.dto.LoginRequest;
import com.finnexa.dto.RegisterRequest;
import com.finnexa.entity.Role;
import com.finnexa.entity.User;
import com.finnexa.exception.BadRequestException;
import com.finnexa.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.auditLogService = auditLogService;
    }

    // Reset administrator password
    @Transactional
    public void resetAdminPassword(String email, String newPassword) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setRole(Role.ADMIN);
        user.setEnabled(true);

        User updatedUser = userRepository.save(user);

        auditLogService.recordAction(
                "ADMIN_PASSWORD_RESET",
                "User",
                updatedUser.getId(),
                "Administrator password reset and account enabled for: "
                        + updatedUser.getEmail()
        );
    }

    // Register a new user
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (request == null
                || request.getFullName() == null
                || request.getFullName().isBlank()
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new BadRequestException(
                    "Full name, email, and password are required."
            );
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException(
                    "An account with this email already exists."
            );
        }

        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CASHIER);
        user.setEnabled(true);

        User savedUser;

        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException(
                    "Unable to register user. Email may already exist."
            );
        }

        auditLogService.recordAction(
                "USER_CREATED",
                "User",
                savedUser.getId(),
                "New user registered: " + savedUser.getEmail()
                        + "; Name: " + savedUser.getFullName()
                        + "; Role: " + savedUser.getRole()
        );

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                "User registered successfully."
        );
    }

    // Update logged-in user's profile
    @Transactional
    public AuthResponse updateProfile(
            String currentEmail,
            com.finnexa.dto.UpdateProfileRequest request) {

        User user = userRepository.findByEmailIgnoreCase(currentEmail)
                .orElseThrow(() -> new BadRequestException("User not found."));

        String previousEmail = user.getEmail();
        String previousFullName = user.getFullName();

        String newEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String newFullName = request.getFullName().trim();

        if (!previousEmail.equalsIgnoreCase(newEmail)
                && userRepository.existsByEmailIgnoreCase(newEmail)) {

            throw new BadRequestException(
                    "An account with this email already exists."
            );
        }

        user.setFullName(newFullName);
        user.setEmail(newEmail);

        User updatedUser = userRepository.save(user);

        auditLogService.recordAction(
                "USER_PROFILE_UPDATED",
                "User",
                updatedUser.getId(),
                "Profile updated for user ID: " + updatedUser.getId()
                        + "; Previous name: " + previousFullName
                        + "; New name: " + updatedUser.getFullName()
                        + "; Previous email: " + previousEmail
                        + "; New email: " + updatedUser.getEmail()
        );

        return new AuthResponse(
                updatedUser.getId(),
                updatedUser.getFullName(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                "Profile updated successfully."
        );
    }

    // Change logged-in user's password
    @Transactional
    public void changePassword(
            String currentEmail,
            com.finnexa.dto.ChangePasswordRequest request) {

        User user = userRepository.findByEmailIgnoreCase(currentEmail)
                .orElseThrow(() -> new BadRequestException("User not found."));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(), user.getPassword())) {

            throw new BadRequestException(
                    "Current password is incorrect."
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(), user.getPassword())) {

            throw new BadRequestException(
                    "New password must be different from the current password."
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        User updatedUser = userRepository.save(user);

        auditLogService.recordAction(
                "USER_PASSWORD_CHANGED",
                "User",
                updatedUser.getId(),
                "Password successfully changed for user: "
                        + updatedUser.getEmail()
        );
    }

    // Login
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new BadRequestException(
                    "Email and password are required."
            );
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BadRequestException(
                        "Unable to retrieve authenticated user."
                ));

        return new AuthResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                "Login successful."
        );
    }
}