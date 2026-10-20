package com.sunset.eveil.global.auth;

import java.util.Base64;

/**
 * X-Kek 헤더 값. Base64를 푼 32바이트 KEK. 서비스가 사용 후 0으로 덮어쓴다.
 */
public record Kek(byte[] bytes) {

	public static final int LENGTH_BYTES = 32;

	public Kek {
		if (bytes == null || bytes.length != LENGTH_BYTES) {
			throw new InvalidAuthHeaderException();
		}
	}

	public static Kek fromBase64(String base64) {
		if (base64 == null) {
			throw new InvalidAuthHeaderException();
		}
		try {
			return new Kek(Base64.getDecoder().decode(base64));
		} catch (IllegalArgumentException e) {
			throw new InvalidAuthHeaderException();
		}
	}
}
