package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateUserRequest;
import com.mse.edu.forum.api.generated.model.UpdateUserRequest;
import com.mse.edu.forum.api.generated.model.UserPage;
import com.mse.edu.forum.api.generated.model.UserResponse;
import com.mse.edu.forum.api.generated.model.RegisterRequest;
import com.mse.edu.forum.api.generated.model.UpdateProfileRequest;
import com.mse.edu.forum.api.generated.model.ChangePasswordRequest;
import com.mse.edu.forum.api.generated.model.ChangeRoleRequest;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.domain.UserRole;
import com.mse.edu.forum.mapper.UserMapper;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.ForumUserDetails;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

	private final UserRepository users;
	private final UserMapper mapper;
	private final PasswordEncoder passwordEncoder;
	private final ModerationAuditService audit;

	public UserService(
			UserRepository users,
			UserMapper mapper,
			PasswordEncoder passwordEncoder,
			ModerationAuditService audit) {
		this.users = users;
		this.mapper = mapper;
		this.passwordEncoder = passwordEncoder;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public UserPage findAll(int page, int size) {
		var result = users.findAll(PageRequest.of(page, size, Sort.by(Sort.Order.asc("id"))));
		boolean hideEmail = TopicService.currentUser().getDomainRole() == UserRole.MODERATOR;
		return new UserPage(
				page, size, result.getTotalElements(), result.getTotalPages(),
				result.stream().map(user -> response(user, hideEmail)).toList());
	}

	@Transactional(readOnly = true)
	public UserResponse findById(Long id) {
		UserEntity entity = requireUser(id);
		ForumUserDetails actor = TopicService.currentUser();
		boolean hideEmail = actor.getDomainRole() == UserRole.MODERATOR && actor.getId() != id;
		return response(entity, hideEmail);
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		validatePassword(request.getPassword());
		UserEntity entity = mapper.toEntity(request);
		normalize(entity);
		if (users.existsByUsernameIgnoreCase(entity.getUsername())) {
			throw conflict("Username already taken");
		}
		if (entity.getEmail() != null && users.existsByEmailIgnoreCase(entity.getEmail())) {
			throw conflict("Email already in use");
		}
		entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		return mapper.toResponse(users.saveAndFlush(entity));
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		validatePassword(request.getPassword());
		UserEntity entity = new UserEntity();
		entity.setUsername(request.getUsername());
		entity.setEmail(request.getEmail());
		entity.setRole(UserRole.USER);
		normalize(entity);
		requireUniqueIdentity(entity.getUsername(), entity.getEmail(), null);
		entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		return mapper.toResponse(users.saveAndFlush(entity));
	}

	@Transactional
	public UserResponse updateCurrentProfile(UpdateProfileRequest request) {
		UserEntity entity = requireUser(TopicService.currentUser().getId());
		String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
		String email = normalizeEmail(request.getEmail());
		requireUniqueIdentity(username, email, entity.getId());
		entity.setUsername(username);
		entity.setEmail(email);
		return mapper.toResponse(users.saveAndFlush(entity));
	}

	@Transactional
	public void changeCurrentPassword(ChangePasswordRequest request) {
		UserEntity entity = requireUser(TopicService.currentUser().getId());
		if (!passwordEncoder.matches(request.getCurrentPassword(), entity.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
		}
		validatePassword(request.getNewPassword());
		entity.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		entity.setAuthVersion(entity.getAuthVersion() + 1);
		users.saveAndFlush(entity);
	}

	@Transactional
	public UserResponse changeRole(Long id, ChangeRoleRequest request) {
		UserEntity entity = requireUser(id);
		if (entity.getRole() == UserRole.ADMIN) {
			throw conflict("Administrator role cannot be changed");
		}
		UserRole role = UserRole.valueOf(request.getRole().getValue());
		entity.setRole(role);
		audit.record(
				TopicService.currentUser().getId(),
				"USER",
				id,
				"ROLE_CHANGE",
				role.name(),
				null,
				false);
		return mapper.toResponse(users.saveAndFlush(entity));
	}

	@Transactional
	public UserResponse update(Long id, UpdateUserRequest request) {
		UserEntity entity = requireUser(id);
		ForumUserDetails actor = TopicService.currentUser();
		UserRole requestedRole = mapper.toDomainRole(request.getRole());
		if (actor.getDomainRole() != UserRole.ADMIN && entity.getRole() != requestedRole) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can change roles");
		}
		if (actor.getId() == id && entity.getRole() == UserRole.ADMIN && requestedRole != UserRole.ADMIN) {
			throw conflict("Administrators cannot demote themselves");
		}
		if (entity.getRole() == UserRole.ADMIN && requestedRole != UserRole.ADMIN) {
			requireAnotherAdmin(id);
		}

		String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
		if (users.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
			throw conflict("Username already taken");
		}
		String email = normalizeEmail(request.getEmail());
		if (email != null && users.existsByEmailIgnoreCaseAndIdNot(email, id)) {
			throw conflict("Email already in use");
		}
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			validatePassword(request.getPassword());
			entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
			entity.setAuthVersion(entity.getAuthVersion() + 1);
		}
		mapper.applyUpdate(request, entity);
		normalize(entity);
		return mapper.toResponse(users.saveAndFlush(entity));
	}

	@Transactional
	public void delete(Long id) {
		UserEntity entity = requireUser(id);
		ForumUserDetails actor = TopicService.currentUser();
		if (actor.getId() == id) {
			throw conflict("Administrators cannot delete themselves");
		}
		if (entity.getRole() == UserRole.ADMIN) {
			requireAnotherAdmin(id);
		}
		entity.setUsername("deleted-user-" + entity.getId());
		entity.setEmail(null);
		entity.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
		entity.setRole(UserRole.USER);
		entity.setAnonymizedAt(java.time.Instant.now());
		entity.setAuthVersion(entity.getAuthVersion() + 1);
		users.saveAndFlush(entity);
		audit.record(actor.getId(), "USER", id, "ANONYMIZE", "ACCOUNT_DELETION", null, false);
	}

	private void requireAnotherAdmin(Long excludedId) {
		long otherAdmins = users.lockAllByRole(UserRole.ADMIN).stream()
				.filter(user -> !user.getId().equals(excludedId))
				.count();
		if (otherAdmins == 0) {
			throw conflict("The final administrator cannot be removed or demoted");
		}
	}

	private UserEntity requireUser(Long id) {
		return users.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private UserResponse response(UserEntity entity, boolean hideEmail) {
		UserResponse response = mapper.toResponse(entity);
		if (hideEmail) {
			response.setEmail(null);
		}
		return response;
	}

	private static void normalize(UserEntity user) {
		user.setUsername(user.getUsername().trim().toLowerCase(Locale.ROOT));
		user.setEmail(normalizeEmail(user.getEmail()));
	}

	private static String normalizeEmail(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static void validatePassword(String password) {
		if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
		}
	}

	private void requireUniqueIdentity(String username, String email, Long excludedId) {
		boolean usernameTaken = excludedId == null
				? users.existsByUsernameIgnoreCase(username)
				: users.existsByUsernameIgnoreCaseAndIdNot(username, excludedId);
		if (usernameTaken) {
			throw conflict("Username already taken");
		}
		boolean emailTaken = excludedId == null
				? users.existsByEmailIgnoreCase(email)
				: users.existsByEmailIgnoreCaseAndIdNot(email, excludedId);
		if (emailTaken) {
			throw conflict("Email already in use");
		}
	}

	private static ResponseStatusException conflict(String message) {
		return new ResponseStatusException(HttpStatus.CONFLICT, message);
	}
}
