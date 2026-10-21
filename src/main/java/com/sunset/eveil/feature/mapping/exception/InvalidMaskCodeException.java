package com.sunset.eveil.feature.mapping.exception;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidMaskCodeException extends BusinessException {

	public InvalidMaskCodeException() {
		super(HttpStatus.BAD_REQUEST, "마스킹 코드 형식이 올바르지 않습니다.");
	}
}
