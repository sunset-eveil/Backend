package com.sunset.eveil.feature.mapping.domain;

/** 저장된 매핑의 Base64 인코딩 값. */
public record EncryptedMapping(String iv, String ciphertext) {

	public EncryptedMapping {
		if (iv == null || iv.isBlank() || ciphertext == null || ciphertext.isBlank()) {
			throw new IllegalArgumentException("Encrypted mapping must not be empty");
		}
	}
}
