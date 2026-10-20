package com.sunset.eveil.global.crypto;

import com.sunset.eveil.global.error.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 복호화·키 언랩 실패. 키 불일치와 위변조를 구분하지 않고, 상세 원인(AEADBadTag 등)도 메시지에 담지 않는다.
 */
public class CryptoException extends BusinessException {

	static final String MESSAGE = "매핑 정보를 열 수 없습니다.";

	public CryptoException() {
		super(HttpStatus.FORBIDDEN, MESSAGE);
	}
}
