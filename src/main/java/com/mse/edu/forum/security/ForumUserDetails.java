package com.mse.edu.forum.security;

import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.domain.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class ForumUserDetails implements UserDetails {

	private final long id;
	private final String username;
	private final String passwordHash;
	private final UserRole domainRole;
	private final boolean loginAllowed;
	private final int authVersion;

	public ForumUserDetails(
			long id,
			String username,
			String passwordHash,
			UserRole domainRole,
			boolean loginAllowed,
			int authVersion) {
		this.id = id;
		this.username = username;
		this.passwordHash = passwordHash == null ? "" : passwordHash;
		this.domainRole = domainRole;
		this.loginAllowed = loginAllowed;
		this.authVersion = authVersion;
	}

	public static ForumUserDetails fromEntity(UserEntity entity) {
		String hash = entity.getPasswordHash();
		boolean canLogin = hash != null && !hash.isEmpty();
		return new ForumUserDetails(
				entity.getId(), entity.getUsername(), hash == null ? "" : hash, entity.getRole(), canLogin, entity.getAuthVersion());
	}

	public long getId() {
		return id;
	}

	public UserRole getDomainRole() {
		return domainRole;
	}

	public int getAuthVersion() {
		return authVersion;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + domainRole.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return loginAllowed;
	}
}
