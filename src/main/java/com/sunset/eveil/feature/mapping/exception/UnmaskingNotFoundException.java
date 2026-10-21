package com.sunset.eveil.feature.mapping.exception;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class UnmaskingNotFoundException extends BusinessException {

	public UnmaskingNotFoundException() {
		super(HttpStatus.NOT_FOUND, "해당 마스킹 정보를 찾을 수 없습니다.");
	}
}
