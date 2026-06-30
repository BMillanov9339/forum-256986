package com.mse.edu.forum.api;

import java.util.Map;

public record ApiErrorResponse(String error, String message, String requestId, Map<String, String> fieldErrors) {

	public ApiErrorResponse(String error, String message, String requestId) {
		this(error, message, requestId, Map.of());
	}
}
