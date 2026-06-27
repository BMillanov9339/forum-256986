package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.UsersApi;
import com.mse.edu.forum.api.generated.model.CreateUserRequest;
import com.mse.edu.forum.api.generated.model.UpdateUserRequest;
import com.mse.edu.forum.api.generated.model.UserResponse;
import com.mse.edu.forum.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
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
	public ResponseEntity<List<UserResponse>> listUsers() {
		log.debug("listUsers invoked");
		return ResponseEntity.ok(userService.findAll());
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','MODERATOR') or @userSecurity.isSelf(#id)")
	public ResponseEntity<UserResponse> getUserById(Long id) {
		log.debug("getUserById invoked id={}", id);
		return userService
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponse> createUser(@Valid CreateUserRequest createUserRequest) {
		log.debug("createUser invoked username={}", createUserRequest.getUsername());
		UserResponse created = userService.create(createUserRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Override
	@PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(#id)")
	public ResponseEntity<UserResponse> updateUser(Long id, @Valid UpdateUserRequest updateUserRequest) {
		log.debug("updateUser invoked id={}", id);
		return userService
				.update(id, updateUserRequest)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteUser(Long id) {
		log.debug("deleteUser invoked id={}", id);
		if (userService.delete(id)) {
			return ResponseEntity.noContent().build();
		}
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
	}
}
