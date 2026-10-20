package com.sunset.eveil.global.auth;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 헤더 누락, 미등록 클라이언트, authKey 불일치. 어느 쪽인지 구분해 알려주지 않는다.
 */
public class ClientAuthenticationException extends BusinessException {

	static final String MESSAGE = "클라이언트 인증에 실패했습니다.";

	public ClientAuthenticationException() {
		super(HttpStatus.UNAUTHORIZED, MESSAGE);
	}
}
