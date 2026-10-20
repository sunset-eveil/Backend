package com.sunset.eveil.global.error;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 응답 body. docs/API.md 공통 사항: { "status": int, "message": string }
 */
public record ErrorResponse(int status, String message) {

	public ErrorResponse {
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("message must not be blank");
		}
	}

	public static ErrorResponse of(HttpStatus status, String message) {
		return new ErrorResponse(status.value(), message);
	}
}
