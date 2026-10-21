package com.sunset.eveil.feature.mapping.exception;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class MappingNotFoundException extends BusinessException {

	public MappingNotFoundException() {
		super(HttpStatus.NOT_FOUND, "해당 대화의 매핑 정보가 없습니다.");
	}
}
