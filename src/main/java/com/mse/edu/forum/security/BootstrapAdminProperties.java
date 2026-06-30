package com.mse.edu.forum.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(String username, String email, String password) {

	public BootstrapAdminProperties {
		if (username == null || !username.matches("[A-Za-z0-9._-]{1,100}")) {
			throw new IllegalArgumentException(
					"app.bootstrap-admin.username must contain 1-100 letters, digits, dots, underscores, or hyphens");
		}
		if (email != null && !email.isBlank()
				&& (email.length() > 320 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
			throw new IllegalArgumentException("app.bootstrap-admin.email must be a valid email address");
		}
		if (password == null || password.length() < 12) {
			throw new IllegalArgumentException("app.bootstrap-admin.password must contain at least 12 characters");
		}
		String normalized = password.toLowerCase(java.util.Locale.ROOT);
		if (normalized.contains("change-me") || normalized.contains("replace-me")) {
			throw new IllegalArgumentException("app.bootstrap-admin.password must not use a placeholder value");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException("app.bootstrap-admin.password must not exceed 72 UTF-8 bytes");
		}
	}
}
