package com.mse.edu.forum.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

	@Test
	void acceptsValidBootstrapCredentials() {
		assertDoesNotThrow(() -> new BootstrapAdminProperties(
				"admin.user", "admin@example.com", "a-secure-admin-password"));
	}

	@Test
	void rejectsInvalidBootstrapIdentityAndPassword() {
		assertThrows(IllegalArgumentException.class,
				() -> new BootstrapAdminProperties("invalid user", "admin@example.com", "a-secure-admin-password"));
		assertThrows(IllegalArgumentException.class,
				() -> new BootstrapAdminProperties("admin", "not-an-email", "a-secure-admin-password"));
		assertThrows(IllegalArgumentException.class,
				() -> new BootstrapAdminProperties("admin", "admin@example.com", "too-short"));
		assertThrows(IllegalArgumentException.class,
				() -> new BootstrapAdminProperties("admin", "admin@example.com", "replace-me-with-a-password"));
	}

	@Test
	void validatesJwtSecretAndExpiration() {
		assertDoesNotThrow(() -> new JwtProperties("a-valid-jwt-secret-with-at-least-32-bytes", 900_000));
		assertThrows(IllegalArgumentException.class,
				() -> new JwtProperties("replace-me-jwt-secret-with-at-least-32-bytes", 900_000));
		assertThrows(IllegalArgumentException.class,
				() -> new JwtProperties("a-valid-jwt-secret-with-at-least-32-bytes", 30_000));
	}
}
