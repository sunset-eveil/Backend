package com.sunset.eveil.global.auth;

/**
 * 인증에 필요한 클라이언트 저장 정보. 서버가 보관하는 것은 authKey 해시와 감싼 AES 키뿐이다.
 */
public record ClientCredentials(String clientId, byte[] authKeyHash, byte[] wrappedAesKey) {

	public ClientCredentials {
		if (clientId == null || clientId.isBlank()) {
			throw new IllegalArgumentException("clientId must not be blank");
		}
		if (authKeyHash == null || authKeyHash.length == 0) {
			throw new IllegalArgumentException("authKeyHash must not be empty");
		}
		if (wrappedAesKey == null || wrappedAesKey.length == 0) {
			throw new IllegalArgumentException("wrappedAesKey must not be empty");
		}
	}
}
