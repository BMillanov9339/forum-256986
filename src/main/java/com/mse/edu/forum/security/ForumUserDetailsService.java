package com.mse.edu.forum.security;

import com.mse.edu.forum.repo.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ForumUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public ForumUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		var user = identifier != null && identifier.contains("@")
				? userRepository.findByEmailIgnoreCase(identifier.trim())
				: userRepository.findByUsernameIgnoreCase(identifier == null ? "" : identifier.trim());
		return user
				.map(ForumUserDetails::fromEntity)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
