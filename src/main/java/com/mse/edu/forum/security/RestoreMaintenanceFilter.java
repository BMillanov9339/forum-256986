package com.mse.edu.forum.security;

import com.mse.edu.forum.maintenance.RestoreMaintenanceState;
import com.mse.edu.forum.maintenance.RestoreProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import com.mse.edu.forum.api.RequestIdFilter;

@Component
public class RestoreMaintenanceFilter extends OncePerRequestFilter {

	private final RestoreMaintenanceState maintenanceState;
	private final List<String> allowlist;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public RestoreMaintenanceFilter(RestoreMaintenanceState maintenanceState, RestoreProperties properties) {
		this.maintenanceState = maintenanceState;
		this.allowlist = properties.allowlist();
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		if (!maintenanceState.isRestoreInProgress() || isAllowlisted(applicationPath(request))) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(maintenanceState.getRetryAfterSeconds()));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("""
				{"error":"RESTORE_IN_PROGRESS","message":"Service temporarily unavailable during restore","requestId":"%s","fieldErrors":{}}
				""".formatted(RequestIdFilter.get(request)));
	}

	private String applicationPath(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
	}

	private boolean isAllowlisted(String requestUri) {
		for (String pattern : allowlist) {
			if (pathMatcher.match(pattern, requestUri)) {
				return true;
			}
		}
		return false;
	}
}
