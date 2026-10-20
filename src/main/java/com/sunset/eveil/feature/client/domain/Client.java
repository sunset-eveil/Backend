package com.sunset.eveil.feature.client.domain;

import java.time.Instant;
import lombok.Getter;

/**
 * 등록된 클라이언트. 서버가 보관하는 것은 authKey 해시와 KEK로 감싼 AES 키뿐이다 (CLAUDE.md 4번).
 */
@Getter
public class Client {

	static final int AUTH_KEY_HASH_LENGTH_BYTES = 32;
	static final int WRAPPED_AES_KEY_LENGTH_BYTES = 40;

	private final String clientId;
	private final byte[] authKeyHash;
	private final byte[] wrappedAesKey;
	private final Instant createdAt;

	private Client(String clientId, byte[] authKeyHash, byte[] wrappedAesKey, Instant createdAt) {
		if (clientId == null || clientId.isBlank()) {
			throw new IllegalArgumentException("clientId must not be blank");
		}
		if (authKeyHash == null || authKeyHash.length != AUTH_KEY_HASH_LENGTH_BYTES) {
			throw new IllegalArgumentException("authKeyHash must be " + AUTH_KEY_HASH_LENGTH_BYTES + " bytes");
		}
		if (wrappedAesKey == null || wrappedAesKey.length != WRAPPED_AES_KEY_LENGTH_BYTES) {
			throw new IllegalArgumentException("wrappedAesKey must be " + WRAPPED_AES_KEY_LENGTH_BYTES + " bytes");
		}
		if (createdAt == null) {
			throw new IllegalArgumentException("createdAt must not be null");
		}
		this.clientId = clientId;
		this.authKeyHash = authKeyHash;
		this.wrappedAesKey = wrappedAesKey;
		this.createdAt = createdAt;
	}

	/** 등록 시점에 새로 만든다. */
	public static Client register(String clientId, byte[] authKeyHash, byte[] wrappedAesKey, Instant now) {
		return new Client(clientId, authKeyHash, wrappedAesKey, now);
	}

	/** 저장소에서 읽은 값을 되살린다. */
	public static Client restore(String clientId, byte[] authKeyHash, byte[] wrappedAesKey, Instant createdAt) {
		return new Client(clientId, authKeyHash, wrappedAesKey, createdAt);
	}
}
