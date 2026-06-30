package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.AuthApi;
import com.mse.edu.forum.api.generated.model.LoginRequest;
import com.mse.edu.forum.api.generated.model.LoginResponse;
import com.mse.edu.forum.service.AuthService;
import com.mse.edu.forum.security.AuthRateLimiter;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthApiController implements AuthApi {

	private static final Logger log = LogManager.getLogger(AuthApiController.class);

	private final AuthService authService;
	private final AuthRateLimiter rateLimiter;

	public AuthApiController(AuthService authService, AuthRateLimiter rateLimiter) {
		this.authService = authService;
		this.rateLimiter = rateLimiter;
	}

	@Override
	public ResponseEntity<LoginResponse> login(@Valid LoginRequest loginRequest) {
		log.debug("login invoked identifier={}", loginRequest.getIdentifier());
		rateLimiter.checkLogin(loginRequest.getIdentifier());
		try {
			LoginResponse response = authService.login(loginRequest);
			rateLimiter.recordLoginSuccess(loginRequest.getIdentifier());
			return ResponseEntity.ok(response);
		} catch (AuthenticationException e) {
			rateLimiter.recordLoginFailure(loginRequest.getIdentifier());
			log.info("login failed identifier={}", loginRequest.getIdentifier());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
	}

	@Override
	public ResponseEntity<com.mse.edu.forum.api.generated.model.UserResponse> getCurrentUser() {
		return ResponseEntity.ok(authService.currentUser());
	}

	@Override
	public ResponseEntity<com.mse.edu.forum.api.generated.model.UserResponse> register(
			@Valid com.mse.edu.forum.api.generated.model.RegisterRequest request) {
		rateLimiter.checkAndRecordRegistration();
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}
}
