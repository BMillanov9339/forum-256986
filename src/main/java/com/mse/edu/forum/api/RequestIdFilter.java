package com.mse.edu.forum.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-ID";
	public static final String ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = request.getHeader(HEADER);
		if (requestId == null || !requestId.matches("[A-Za-z0-9._-]{1,100}")) {
			requestId = UUID.randomUUID().toString();
		}
		request.setAttribute(ATTRIBUTE, requestId);
		response.setHeader(HEADER, requestId);
		ThreadContext.put("requestId", requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			ThreadContext.remove("requestId");
		}
	}

	public static String get(HttpServletRequest request) {
		Object value = request.getAttribute(ATTRIBUTE);
		return value == null ? "unknown" : value.toString();
	}
}
