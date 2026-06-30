package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.UsersApi;
import com.mse.edu.forum.api.generated.model.CreateUserRequest;
import com.mse.edu.forum.api.generated.model.UpdateUserRequest;
import com.mse.edu.forum.api.generated.model.UserResponse;
import com.mse.edu.forum.service.UserService;
import jakarta.validation.Valid;
import com.mse.edu.forum.api.generated.model.UserPage;
import com.mse.edu.forum.api.generated.model.UpdateProfileRequest;
import com.mse.edu.forum.api.generated.model.ChangePasswordRequest;
import com.mse.edu.forum.api.generated.model.ChangeRoleRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UsersApiController implements UsersApi {

	private static final Logger log = LogManager.getLogger(UsersApiController.class);

	private final UserService userService;

	public UsersApiController(UserService userService) {
		this.userService = userService;
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
	public ResponseEntity<UserPage> listUsers(Integer page, Integer size) {
		log.debug("listUsers invoked");
		return ResponseEntity.ok(userService.findAll(page, size));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','MODERATOR') or @userSecurity.isSelf(#id)")
	public ResponseEntity<UserResponse> getUserById(Long id) {
		log.debug("getUserById invoked id={}", id);
		return ResponseEntity.ok(userService.findById(id));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> createUser(@Valid CreateUserRequest createUserRequest) {
		log.debug("createUser invoked username={}", createUserRequest.getUsername());
		UserResponse created = userService.create(createUserRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> updateUser(Long id, @Valid UpdateUserRequest updateUserRequest) {
		log.debug("updateUser invoked id={}", id);
		return ResponseEntity.ok(userService.update(id, updateUserRequest));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteUser(Long id) {
		log.debug("deleteUser invoked id={}", id);
		userService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserResponse> updateCurrentUser(@Valid UpdateProfileRequest request) {
		return ResponseEntity.ok(userService.updateCurrentProfile(request));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> changeCurrentUserPassword(@Valid ChangePasswordRequest request) {
		userService.changeCurrentPassword(request);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> changeUserRole(Long id, @Valid ChangeRoleRequest request) {
		return ResponseEntity.ok(userService.changeRole(id, request));
	}
}
