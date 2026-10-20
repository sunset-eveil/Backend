package com.sunset.eveil.global.crypto;

import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 등록 시 AES-256 키 생성. 생성된 키는 KEK로 감싼 형태로만 저장한다.
 */
@Component
public class AesKeyGenerator {

	private static final String KEY_ALGORITHM = "AES";
	private static final int KEY_LENGTH_BITS = 256;

	public SecretKey generate() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
			keyGenerator.init(KEY_LENGTH_BITS);
			return keyGenerator.generateKey();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("AES KeyGenerator unavailable");
		}
	}
}
