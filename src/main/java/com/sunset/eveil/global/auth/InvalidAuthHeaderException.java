package com.sunset.eveil.global.auth;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 인증 헤더 형식 오류 (clientId가 UUID가 아님, authKey가 Base64가 아님).
 */
public class InvalidAuthHeaderException extends BusinessException {

	static final String MESSAGE = "요청 형식이 올바르지 않습니다.";

	public InvalidAuthHeaderException() {
		super(HttpStatus.BAD_REQUEST, MESSAGE);
	}
}
