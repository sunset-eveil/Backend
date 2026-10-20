package com.sunset.eveil.global.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

/**
 * authKey 해시 생성·비교. 비교는 타이밍 공격을 막기 위해 MessageDigest.isEqual만 사용한다.
 */
@Component
public class Sha256Hasher {

	private static final String ALGORITHM = "SHA-256";

	public byte[] hash(byte[] input) {
		try {
			return MessageDigest.getInstance(ALGORITHM).digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable");
		}
	}

	public boolean matches(byte[] input, byte[] expectedHash) {
		return MessageDigest.isEqual(hash(input), expectedHash);
	}
}
