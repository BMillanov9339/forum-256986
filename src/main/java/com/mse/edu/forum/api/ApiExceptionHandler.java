package com.mse.edu.forum.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("VALIDATION_ERROR", message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		String message = ex.getConstraintViolations().stream()
				.findFirst()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("VALIDATION_ERROR", message));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
		return ResponseEntity.status(status).body(new ApiErrorResponse(errorCode(status), message));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiErrorResponse("FORBIDDEN", "Access denied"));
	}

	private static String errorCode(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> "BAD_REQUEST";
			case UNAUTHORIZED -> "UNAUTHORIZED";
			case FORBIDDEN -> "FORBIDDEN";
			case NOT_FOUND -> "NOT_FOUND";
			case CONFLICT -> "CONFLICT";
			case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
			default -> "ERROR";
		};
	}
}
