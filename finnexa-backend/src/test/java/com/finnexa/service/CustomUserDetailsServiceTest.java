package com.finnexa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.finnexa.entity.Role;
import com.finnexa.entity.User;
import com.finnexa.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;
	private User user;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setEmail("ansar@example.com");
		user.setPassword("encodedPassword123");
		user.setRole(Role.CASHIER);
		user.setEnabled(true);
	}

	@Test
	void loadUserByUsername_shouldReturnUserDetailsWhenUserExists() {
		when(userRepository.findByEmailIgnoreCase("ansar@example.com")).thenReturn(Optional.of(user));
		UserDetails userDetails = customUserDetailsService.loadUserByUsername("ansar@example.com");
		assertEquals("ansar@example.com", userDetails.getUsername());
		assertEquals("encodedPassword123", userDetails.getPassword());
		assertTrue(userDetails.isEnabled());
		assertTrue(userDetails.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_CASHIER")));
		verify(userRepository).findByEmailIgnoreCase("ansar@example.com");
	}

	@Test
	void loadUserByUsername_shouldTrimEmailBeforeSearching() {
		when(userRepository.findByEmailIgnoreCase("ansar@example.com")).thenReturn(Optional.of(user));
		UserDetails userDetails = customUserDetailsService.loadUserByUsername(" ansar@example.com ");
		assertEquals("ansar@example.com", userDetails.getUsername());
		verify(userRepository).findByEmailIgnoreCase("ansar@example.com");
	}

	@Test
	void loadUserByUsername_shouldThrowExceptionWhenUserDoesNotExist() {
		when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());
		UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
				() -> customUserDetailsService.loadUserByUsername("unknown@example.com"));
		assertEquals("User not found with email: unknown@example.com", exception.getMessage());
		verify(userRepository).findByEmailIgnoreCase("unknown@example.com");
	}

	@Test
	void loadUserByUsername_shouldMarkDisabledUserAsDisabled() {
		user.setEnabled(false);
		when(userRepository.findByEmailIgnoreCase("ansar@example.com")).thenReturn(Optional.of(user));
		UserDetails userDetails = customUserDetailsService.loadUserByUsername("ansar@example.com");
		assertFalse(userDetails.isEnabled());
		assertTrue(userDetails.isAccountNonLocked());
		assertTrue(userDetails.isAccountNonExpired());
		assertTrue(userDetails.isCredentialsNonExpired());
	}

	@Test
	void loadUserByUsername_shouldReturnCorrectRoleAuthority() {
		user.setRole(Role.ADMIN);
		when(userRepository.findByEmailIgnoreCase("ansar@example.com")).thenReturn(Optional.of(user));
		UserDetails userDetails = customUserDetailsService.loadUserByUsername("ansar@example.com");
		assertTrue(userDetails.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
	}
}