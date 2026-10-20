package com.sunset.eveil.feature.client.exception;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * authKey·kek가 Base64가 아니거나, kek가 32바이트가 아닌 경우.
 */
public class InvalidClientRequestException extends BusinessException {

	static final String MESSAGE = "요청 형식이 올바르지 않습니다.";

	public InvalidClientRequestException() {
		super(HttpStatus.BAD_REQUEST, MESSAGE);
	}
}
