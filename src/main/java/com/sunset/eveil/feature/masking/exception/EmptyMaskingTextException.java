package com.sunset.eveil.feature.masking.exception;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class EmptyMaskingTextException extends BusinessException {

	static final String MESSAGE = "마스킹할 텍스트가 비어 있습니다.";

	public EmptyMaskingTextException() {
		super(HttpStatus.BAD_REQUEST, MESSAGE);
	}
}
