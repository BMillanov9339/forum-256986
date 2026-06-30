package com.mse.edu.forum.security;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthRateLimiter {

	private static final int MAX_TRACKED_KEYS = 10_000;
	private static final int LOGIN_FAILURE_LIMIT = 10;
	private static final int LOGIN_IP_FAILURE_LIMIT = 30;
	private static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);
	private static final int REGISTRATION_LIMIT = 5;
	private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);

	private final Clock clock;
	private final Map<String, Window> windows = new LinkedHashMap<>();

	public AuthRateLimiter() {
		this(Clock.systemUTC());
	}

	AuthRateLimiter(Clock clock) {
		this.clock = clock;
	}

	public synchronized void checkLogin(String identifier) {
		check("login-account:" + key(identifier, false), LOGIN_FAILURE_LIMIT, LOGIN_WINDOW);
		check("login-ip:" + key("", true), LOGIN_IP_FAILURE_LIMIT, LOGIN_WINDOW);
	}

	public synchronized void recordLoginFailure(String identifier) {
		increment("login-account:" + key(identifier, false), LOGIN_WINDOW);
		increment("login-ip:" + key("", true), LOGIN_WINDOW);
	}

	public synchronized void recordLoginSuccess(String identifier) {
		windows.remove("login-account:" + key(identifier, false));
	}

	public synchronized void checkAndRecordRegistration() {
		String key = "register:" + key("", true);
		check(key, REGISTRATION_LIMIT, REGISTRATION_WINDOW);
		increment(key, REGISTRATION_WINDOW);
	}

	private void check(String key, int limit, Duration duration) {
		Window window = activeWindow(key, duration);
		if (window != null && window.count >= limit) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many authentication attempts");
		}
	}

	private void increment(String key, Duration duration) {
		Window window = activeWindow(key, duration);
		if (window == null) {
			evictIfNecessary();
			windows.put(key, new Window(Instant.now(clock), 1));
		} else {
			window.count++;
		}
	}

	private Window activeWindow(String key, Duration duration) {
		Window window = windows.get(key);
		if (window != null && window.started.plus(duration).isBefore(Instant.now(clock))) {
			windows.remove(key);
			return null;
		}
		return window;
	}

	private void evictIfNecessary() {
		if (windows.size() >= MAX_TRACKED_KEYS) {
			String oldest = windows.keySet().iterator().next();
			windows.remove(oldest);
		}
	}

	private String key(String identifier, boolean includeAddress) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
				.getRequest();
		String normalizedIdentifier = identifier.trim().toLowerCase(java.util.Locale.ROOT);
		String material = includeAddress ? request.getRemoteAddr() : normalizedIdentifier;
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
			return java.util.HexFormat.of().formatHex(digest);
		} catch (java.security.NoSuchAlgorithmException impossible) {
			throw new IllegalStateException(impossible);
		}
	}

	private static final class Window {
		private final Instant started;
		private int count;

		private Window(Instant started, int count) {
			this.started = started;
			this.count = count;
		}
	}
}
