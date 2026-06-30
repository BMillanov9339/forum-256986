package com.mse.edu.forum.security;

import com.mse.edu.forum.repo.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
			String token = header.substring(7).trim();
			if (!token.isEmpty()) {
				try {
					Claims claims = jwtService.parseAndValidate(token);
					Long uid = claims.get("uid", Long.class);
					Integer version = claims.get("ver", Integer.class);
					if (uid != null && version != null) {
						var user = userRepository.findById(uid).orElse(null);
						if (user != null && user.getAuthVersion() == version) {
							ForumUserDetails principal = ForumUserDetails.fromEntity(user);
						var auth = new UsernamePasswordAuthenticationToken(
								principal, null, principal.getAuthorities());
						SecurityContextHolder.getContext().setAuthentication(auth);
						}
					}
				} catch (JwtException | IllegalArgumentException ignored) {
					SecurityContextHolder.clearContext();
				}
			}
		}
		filterChain.doFilter(request, response);
	}
}
