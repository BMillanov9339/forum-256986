package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.LoginRequest;
import com.mse.edu.forum.api.generated.model.LoginResponse;
import com.mse.edu.forum.api.generated.model.UserResponse;
import com.mse.edu.forum.api.generated.model.RegisterRequest;
import com.mse.edu.forum.security.ForumUserDetails;
import com.mse.edu.forum.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserService userService;

	public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserService userService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userService = userService;
	}

	public LoginResponse login(LoginRequest request) {
		var token =
				UsernamePasswordAuthenticationToken.unauthenticated(request.getIdentifier(), request.getPassword());
		var auth = authenticationManager.authenticate(token);
		var user = (ForumUserDetails) auth.getPrincipal();
		String jwt = jwtService.createToken(
				user.getId(), user.getUsername(), user.getDomainRole(), user.getAuthVersion());
		return new LoginResponse(jwt, "Bearer", jwtService.getExpiresInSeconds());
	}

	public UserResponse currentUser() {
		return userService.findById(TopicService.currentUser().getId());
	}

	public UserResponse register(RegisterRequest request) {
		return userService.register(request);
	}
}
