package com.sunset.eveil.global.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 에러 응답 통일. 500 응답과 로그에는 예외 메시지 원문을 싣지 않는다 (원문·키가 섞여 있을 수 있음).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	static final String INVALID_REQUEST_MESSAGE = "요청 형식이 올바르지 않습니다.";
	static final String AUTH_FAILED_MESSAGE = "클라이언트 인증에 실패했습니다.";
	static final String NOT_FOUND_MESSAGE = "요청한 리소스를 찾을 수 없습니다.";
	static final String METHOD_NOT_ALLOWED_MESSAGE = "허용되지 않은 메서드입니다.";
	static final String INTERNAL_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		return respond(e.getStatus(), e.getMessage());
	}

	@ExceptionHandler({
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class,
			MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
		return respond(HttpStatus.BAD_REQUEST, INVALID_REQUEST_MESSAGE);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
		return respond(HttpStatus.UNAUTHORIZED, AUTH_FAILED_MESSAGE);
	}

	@ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
	public ResponseEntity<ErrorResponse> handleNotFound(Exception e) {
		return respond(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		return respond(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_MESSAGE);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		// 메시지·스택트레이스에 원문이 섞일 수 있으므로 예외 클래스 이름만 남긴다.
		log.error("Unhandled exception: {}", e.getClass().getName());
		return respond(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
	}

	private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
	}
}
