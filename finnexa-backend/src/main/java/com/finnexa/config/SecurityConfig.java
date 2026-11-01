package com.finnexa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.cors(Customizer.withDefaults())

				.csrf(csrf -> csrf.disable())

				.httpBasic(Customizer.withDefaults())

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth

						// Allow CORS preflight requests
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						// Public endpoints
						.requestMatchers("/api/auth/login", "/api/health").permitAll()

						// Only ADMIN can register new users
						.requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")

						// User management: ADMIN only
						.requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")

						// Audit logs: ADMIN only
						.requestMatchers("/api/audit-logs", "/api/audit-logs/**").hasRole("ADMIN")

						// Financial reports: ADMIN only
						.requestMatchers("/api/reports", "/api/reports/**").hasRole("ADMIN")

						// Inventory adjustments
						.requestMatchers("/api/inventory/adjustments", "/api/inventory/adjustments/**")
						.hasAnyRole("ADMIN", "INVENTORY_MANAGER")

						// Product read access
						.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**")
						.hasAnyRole("ADMIN", "CASHIER", "INVENTORY_MANAGER")

						// Product creation
						.requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**")
						.hasAnyRole("ADMIN", "INVENTORY_MANAGER")

						// Product updates
						.requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**")
						.hasAnyRole("ADMIN", "INVENTORY_MANAGER")

						// Product deletion: ADMIN only
						.requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**").hasRole("ADMIN")

						// Inventory read access
						.requestMatchers(HttpMethod.GET, "/api/inventory", "/api/inventory/**")
						.hasAnyRole("ADMIN", "CASHIER", "INVENTORY_MANAGER")

						// All remaining endpoints require authentication
						.anyRequest().authenticated());

		return http.build();
	}

}
