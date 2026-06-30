package com.mse.edu.forum.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("app.jwt.secret must be set");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("app.jwt.secret must contain at least 32 UTF-8 bytes");
		}
		String normalized = secret.toLowerCase(java.util.Locale.ROOT);
		if (normalized.contains("change-me") || normalized.contains("replace-me")) {
			throw new IllegalArgumentException("app.jwt.secret must not use a placeholder value");
		}
		if (expirationMs < 60_000 || expirationMs > 86_400_000) {
			throw new IllegalArgumentException("app.jwt.expiration-ms must be between 60000 and 86400000");
		}
	}
}
