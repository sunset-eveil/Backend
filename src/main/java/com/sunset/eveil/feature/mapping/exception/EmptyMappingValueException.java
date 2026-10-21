package com.sunset.eveil.feature.mapping.exception;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class EmptyMappingValueException extends BusinessException {

	public EmptyMappingValueException() {
		super(HttpStatus.BAD_REQUEST, "지정할 값이 비어 있습니다.");
	}
}
