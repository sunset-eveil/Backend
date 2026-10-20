package com.sunset.eveil.feature.mapping.presentation.dto;

/** 모든 필드는 Base64 인코딩 값이다. */
public record GetMappingResponse(String wrappedAesKey, String iv, String ciphertext) {

	public GetMappingResponse {
		if (wrappedAesKey == null || wrappedAesKey.isBlank()
				|| iv == null || iv.isBlank() || ciphertext == null || ciphertext.isBlank()) {
			throw new IllegalArgumentException("Mapping response must not be empty");
		}
	}
}
