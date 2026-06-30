package com.mse.edu.forum.security;

import com.mse.edu.forum.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey signingKey;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createToken(long userId, String username, UserRole role, int authVersion) {
		Instant now = Instant.now();
		Instant exp = now.plusMillis(properties.expirationMs());
		return Jwts.builder()
				.subject(username)
				.claim("uid", userId)
				.claim("role", role.name())
				.claim("ver", authVersion)
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(signingKey)
				.compact();
	}

	public Claims parseAndValidate(String token) throws JwtException {
		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}

	public long getExpiresInSeconds() {
		return Math.max(1L, properties.expirationMs() / 1000L);
	}
}
