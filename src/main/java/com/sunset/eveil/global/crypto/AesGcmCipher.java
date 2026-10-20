package com.sunset.eveil.global.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.springframework.stereotype.Component;

/**
 * 매핑 JSON 암·복호화. AES/GCM/NoPadding, 인증 태그 128비트.
 * IV는 암호화할 때마다 SecureRandom으로 새로 만든다 (같은 키 + 같은 IV 재사용 금지).
 * Cipher는 스레드 안전하지 않으므로 호출마다 새로 만든다.
 */
@Component
public class AesGcmCipher {

	static final int IV_LENGTH_BYTES = 12;
	static final int TAG_LENGTH_BITS = 128;
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private final SecureRandom secureRandom = new SecureRandom();

	public EncryptedPayload encrypt(SecretKey aesKey, byte[] plaintext) {
		byte[] iv = new byte[IV_LENGTH_BYTES];
		secureRandom.nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
			return new EncryptedPayload(iv, cipher.doFinal(plaintext));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("AES-GCM encryption failed");
		}
	}

	/**
	 * @throws CryptoException 키가 다르거나 암호문·IV가 변조된 경우 (AEADBadTagException 포함)
	 */
	public byte[] decrypt(SecretKey aesKey, EncryptedPayload payload) {
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, payload.iv()));
			return cipher.doFinal(payload.ciphertext());
		} catch (GeneralSecurityException e) {
			throw new CryptoException();
		}
	}
}
