package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.AuthApi;
import com.mse.edu.forum.api.generated.model.LoginRequest;
import com.mse.edu.forum.api.generated.model.LoginResponse;
import com.mse.edu.forum.service.AuthService;
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

	public AuthApiController(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public ResponseEntity<LoginResponse> login(@Valid LoginRequest loginRequest) {
		log.debug("login invoked username={}", loginRequest.getUsername());
		try {
			return ResponseEntity.ok(authService.login(loginRequest));
		} catch (AuthenticationException e) {
			log.debug("login failed: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}
	}
}
