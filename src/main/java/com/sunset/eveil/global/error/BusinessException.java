package com.sunset.eveil.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 모든 feature 전용 예외의 부모. 응답 상태 코드와 사용자에게 보여줄 메시지만 가진다.
 * 메시지에 원문·키·내부 원인을 담지 않는다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final HttpStatus status;

	public BusinessException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}
}
