package com.mse.edu.forum.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LogManager.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> fields = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", fields, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException ex, HttpServletRequest request) {
		Map<String, String> fields = new LinkedHashMap<>();
		ex.getConstraintViolations().forEach(v -> fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", fields, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed JSON or invalid value", Map.of(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleIntegrity(
			DataIntegrityViolationException ex, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data", Map.of(), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethod(HttpServletRequest request) {
		return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method not supported", Map.of(), request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMediaType(HttpServletRequest request) {
		return error(
				HttpStatus.UNSUPPORTED_MEDIA_TYPE,
				"UNSUPPORTED_MEDIA_TYPE",
				"Media type not supported",
				Map.of(),
				request);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(
			ResponseStatusException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
		return error(status, errorCode(status), message, Map.of(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", Map.of(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled API exception requestId={}", RequestIdFilter.get(request), ex);
		return error(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR",
				"An unexpected error occurred",
				Map.of(),
				request);
	}

	private static ResponseEntity<ApiErrorResponse> error(
			HttpStatus status,
			String code,
			String message,
			Map<String, String> fields,
			HttpServletRequest request) {
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(code, message, RequestIdFilter.get(request), fields));
	}

	private static String errorCode(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> "BAD_REQUEST";
			case UNAUTHORIZED -> "UNAUTHORIZED";
			case FORBIDDEN -> "FORBIDDEN";
			case NOT_FOUND -> "NOT_FOUND";
			case CONFLICT -> "CONFLICT";
			case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
			case TOO_MANY_REQUESTS -> "RATE_LIMITED";
			default -> "ERROR";
		};
	}
}
