package com.sunset.eveil.global.auth;

/**
 * 인증을 통과한 클라이언트. 인터셉터가 요청 속성에 넣고, ArgumentResolver가 컨트롤러 파라미터로 꺼낸다.
 * wrappedAesKey는 수정 API에서 X-Kek로 풀어 쓰기 위해 함께 전달한다.
 */
public record AuthenticatedClient(String clientId, byte[] wrappedAesKey) {

	static final String REQUEST_ATTRIBUTE = AuthenticatedClient.class.getName();

	public AuthenticatedClient {
		if (clientId == null || clientId.isBlank()) {
			throw new IllegalArgumentException("clientId must not be blank");
		}
		if (wrappedAesKey == null || wrappedAesKey.length == 0) {
			throw new IllegalArgumentException("wrappedAesKey must not be empty");
		}
	}
}
