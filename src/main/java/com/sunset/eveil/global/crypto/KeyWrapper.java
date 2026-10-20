package com.sunset.eveil.global.crypto;

import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * KEK로 클라이언트 AES 키를 감싸고(wrap) 푼다(unwrap). AES Key Wrap (RFC 3394), IV 불필요.
 * 32바이트 AES-256 키 → 40바이트 wrappedAesKey.
 */
@Component
public class KeyWrapper {

	static final int KEK_LENGTH_BYTES = 32;
	static final int WRAPPED_KEY_LENGTH_BYTES = 40;
	private static final String TRANSFORMATION = "AESWrap";
	private static final String KEY_ALGORITHM = "AES";

	public byte[] wrap(byte[] kek, SecretKey aesKey) {
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.WRAP_MODE, toKey(kek));
			return cipher.wrap(aesKey);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES key wrap failed");
		}
	}

	/**
	 * @throws CryptoException KEK가 다르거나 wrappedAesKey가 변조된 경우
	 */
	public SecretKey unwrap(byte[] kek, byte[] wrappedAesKey) {
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.UNWRAP_MODE, toKey(kek));
			return (SecretKey) cipher.unwrap(wrappedAesKey, KEY_ALGORITHM, Cipher.SECRET_KEY);
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new CryptoException();
		}
	}

	private SecretKey toKey(byte[] kek) {
		if (kek == null || kek.length != KEK_LENGTH_BYTES) {
			throw new CryptoException();
		}
		return new SecretKeySpec(kek, KEY_ALGORITHM);
	}
}
