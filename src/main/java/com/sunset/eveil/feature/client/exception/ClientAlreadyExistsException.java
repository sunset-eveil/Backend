package com.sunset.eveil.feature.client.exception;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

public class ClientAlreadyExistsException extends BusinessException {

	static final String MESSAGE = "이미 등록된 클라이언트입니다.";

	public ClientAlreadyExistsException() {
		super(HttpStatus.CONFLICT, MESSAGE);
	}
}
