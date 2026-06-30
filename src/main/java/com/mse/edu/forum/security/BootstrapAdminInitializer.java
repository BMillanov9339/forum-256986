package com.mse.edu.forum.security;

import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.domain.UserRole;
import com.mse.edu.forum.repo.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

	private static final String LEGACY_DEFAULT_HASH =
			"$2a$10$k/5TFC9kQu4R4QAtp0GXkeFQQ7G1Xjk2ZrXKHFGzsOy7GLsiZQGVm";
	private static final String BOOTSTRAP_REQUIRED_HASH = "!bootstrap-secret-required!";

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final BootstrapAdminProperties properties;

	public BootstrapAdminInitializer(
			UserRepository users, PasswordEncoder passwordEncoder, BootstrapAdminProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		UserEntity admin = users.findByUsernameIgnoreCase(properties.username())
				.orElseGet(() -> users.findByUsernameIgnoreCase("admin")
						.filter(user -> BOOTSTRAP_REQUIRED_HASH.equals(user.getPasswordHash())
								|| LEGACY_DEFAULT_HASH.equals(user.getPasswordHash()))
						.orElseGet(() -> users.findByUsernameIgnoreCase("__bootstrap_admin_v7__")
								.filter(user -> BOOTSTRAP_REQUIRED_HASH.equals(user.getPasswordHash()))
								.orElseGet(UserEntity::new)));
		boolean isNew = admin.getId() == null;
		boolean needsBootstrap = isNew
				|| LEGACY_DEFAULT_HASH.equals(admin.getPasswordHash())
				|| BOOTSTRAP_REQUIRED_HASH.equals(admin.getPasswordHash());
		if (needsBootstrap) {
			admin.setUsername(properties.username().trim());
			admin.setEmail(normalizeEmail(properties.email()));
			admin.setRole(UserRole.ADMIN);
			admin.setPasswordHash(passwordEncoder.encode(properties.password()));
			users.save(admin);
		}
	}

	private static String normalizeEmail(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim().toLowerCase(java.util.Locale.ROOT);
	}
}
