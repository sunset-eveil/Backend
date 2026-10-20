package com.sunset.eveil.feature.client.application;

/**
 * 등록 유스케이스 입력. authKey·kek는 Base64를 푼 원본 바이트이며, 서비스가 사용 후 0으로 덮어쓴다.
 */
public record RegisterClientCommand(String clientId, byte[] authKey, byte[] kek) {

	public static final int KEK_LENGTH_BYTES = 32;

	public RegisterClientCommand {
		if (clientId == null || clientId.isBlank()) {
			throw new IllegalArgumentException("clientId must not be blank");
		}
		if (authKey == null || authKey.length == 0) {
			throw new IllegalArgumentException("authKey must not be empty");
		}
		if (kek == null || kek.length != KEK_LENGTH_BYTES) {
			throw new IllegalArgumentException("kek must be " + KEK_LENGTH_BYTES + " bytes");
		}
	}
}
