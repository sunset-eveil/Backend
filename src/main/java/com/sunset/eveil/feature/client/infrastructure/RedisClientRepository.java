package com.sunset.eveil.feature.client.infrastructure;

import com.sunset.eveil.feature.client.domain.Client;
import com.sunset.eveil.feature.client.domain.ClientRepository;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * client:{clientId} Hash. 필드: authKeyHash(Base64), wrappedAesKey(Base64), createdAt(ISO-8601). TTL 없음.
 */
@Repository
@RequiredArgsConstructor
public class RedisClientRepository implements ClientRepository {

	private static final String KEY_PREFIX = "client:";
	private static final String FIELD_AUTH_KEY_HASH = "authKeyHash";
	private static final String FIELD_WRAPPED_AES_KEY = "wrappedAesKey";
	private static final String FIELD_CREATED_AT = "createdAt";

	private final StringRedisTemplate redisTemplate;

	@Override
	public boolean exists(String clientId) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key(clientId)));
	}

	@Override
	public void save(Client client) {
		Base64.Encoder encoder = Base64.getEncoder();
		redisTemplate.opsForHash().putAll(key(client.getClientId()), Map.of(
				FIELD_AUTH_KEY_HASH, encoder.encodeToString(client.getAuthKeyHash()),
				FIELD_WRAPPED_AES_KEY, encoder.encodeToString(client.getWrappedAesKey()),
				FIELD_CREATED_AT, client.getCreatedAt().toString()
		));
	}

	@Override
	public Optional<Client> findById(String clientId) {
		Map<Object, Object> fields = redisTemplate.opsForHash().entries(key(clientId));
		if (fields.isEmpty()) {
			return Optional.empty();
		}
		Base64.Decoder decoder = Base64.getDecoder();
		return Optional.of(Client.restore(
				clientId,
				decoder.decode((String) fields.get(FIELD_AUTH_KEY_HASH)),
				decoder.decode((String) fields.get(FIELD_WRAPPED_AES_KEY)),
				Instant.parse((String) fields.get(FIELD_CREATED_AT))
		));
	}

	private String key(String clientId) {
		return KEY_PREFIX + clientId;
	}
}
