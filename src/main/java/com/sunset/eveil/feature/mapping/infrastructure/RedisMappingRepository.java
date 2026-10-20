package com.sunset.eveil.feature.mapping.infrastructure;

import com.sunset.eveil.feature.mapping.domain.MappingRepository;
import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.global.crypto.AesGcmCipher;
import com.sunset.eveil.global.crypto.EncryptedPayload;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * mapping:{clientId}:{chatId} Hash. 필드: iv(Base64), ciphertext(Base64), updatedAt(ISO-8601).
 * TTL은 생성 시점 기준 7일이며 갱신 시 연장하지 않는다 (CLAUDE.md 2번).
 */
@Repository
@RequiredArgsConstructor
public class RedisMappingRepository implements MappingRepository {

	static final Duration TTL = Duration.ofDays(7);
	private static final String KEY_PREFIX = "mapping:";
	private static final String FIELD_IV = "iv";
	private static final String FIELD_CIPHERTEXT = "ciphertext";
	private static final String FIELD_UPDATED_AT = "updatedAt";

	private final StringRedisTemplate redisTemplate;
	private final AesGcmCipher aesGcmCipher;
	private final JacksonMappingTableCodec codec;

	@Override
	public Optional<MappingTable> find(String clientId, String chatId, SecretKey aesKey) {
		Map<Object, Object> fields = redisTemplate.opsForHash().entries(key(clientId, chatId));
		if (fields.isEmpty()) {
			return Optional.empty();
		}
		Base64.Decoder decoder = Base64.getDecoder();
		EncryptedPayload payload = new EncryptedPayload(
				decoder.decode((String) fields.get(FIELD_IV)),
				decoder.decode((String) fields.get(FIELD_CIPHERTEXT)));
		return Optional.of(codec.decode(aesGcmCipher.decrypt(aesKey, payload)));
	}

	@Override
	public void save(String clientId, String chatId, MappingTable table, SecretKey aesKey) {
		String key = key(clientId, chatId);
		EncryptedPayload payload = aesGcmCipher.encrypt(aesKey, codec.encode(table));
		Base64.Encoder encoder = Base64.getEncoder();

		boolean existed = Boolean.TRUE.equals(redisTemplate.hasKey(key));
		redisTemplate.opsForHash().putAll(key, Map.of(
				FIELD_IV, encoder.encodeToString(payload.iv()),
				FIELD_CIPHERTEXT, encoder.encodeToString(payload.ciphertext()),
				FIELD_UPDATED_AT, Instant.now().toString()
		));
		if (!existed) {
			redisTemplate.expire(key, TTL);
		}
	}

	private String key(String clientId, String chatId) {
		return KEY_PREFIX + clientId + ":" + chatId;
	}
}
