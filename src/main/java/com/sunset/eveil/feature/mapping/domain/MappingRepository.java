package com.sunset.eveil.feature.mapping.domain;

import java.util.Optional;
import javax.crypto.SecretKey;

/**
 * mapping:{clientId}:{chatId} 저장소. 구현이 JSON 직렬화와 AES-GCM 암·복호화를 맡으므로
 * 호출자는 평문 MappingTable과 AES 키만 다룬다. AES 키는 요청 범위에서만 존재한다.
 */
public interface MappingRepository {

	/** 없으면 empty. 키가 다르거나 변조되었으면 CryptoException(403). */
	Optional<MappingTable> find(String clientId, String chatId, SecretKey aesKey);

	/** 저장할 때마다 새 IV로 암호화한다. 신규 생성이면 TTL 7일을 건다. */
	void save(String clientId, String chatId, MappingTable table, SecretKey aesKey);
}
