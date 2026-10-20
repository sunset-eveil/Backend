package com.sunset.eveil.global.crypto;

/**
 * AES-GCM 암호화 결과. iv는 12바이트, ciphertext는 암호문 + 인증 태그(16바이트).
 * Base64 인코딩은 저장·응답하는 쪽에서 한다.
 */
public record EncryptedPayload(byte[] iv, byte[] ciphertext) {

	public EncryptedPayload {
		if (iv == null || iv.length != AesGcmCipher.IV_LENGTH_BYTES) {
			throw new IllegalArgumentException("iv must be " + AesGcmCipher.IV_LENGTH_BYTES + " bytes");
		}
		if (ciphertext == null || ciphertext.length < AesGcmCipher.TAG_LENGTH_BITS / 8) {
			throw new IllegalArgumentException("ciphertext must contain the authentication tag");
		}
	}
}
