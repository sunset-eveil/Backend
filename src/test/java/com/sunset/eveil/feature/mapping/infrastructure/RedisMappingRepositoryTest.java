package com.sunset.eveil.feature.mapping.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sunset.eveil.feature.mapping.domain.EncryptedMapping;
import com.sunset.eveil.global.crypto.AesGcmCipher;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisMappingRepositoryTest {

	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
	@SuppressWarnings("unchecked")
	private final HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
	private final AesGcmCipher cipher = mock(AesGcmCipher.class);
	private final JacksonMappingTableCodec codec = mock(JacksonMappingTableCodec.class);
	private final RedisMappingRepository repository = new RedisMappingRepository(redisTemplate, cipher, codec);

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForHash()).thenReturn(hashes);
	}

	@Test
	void 클라이언트와_대화의_Hash에서_암호문을_변경_없이_읽고_TTL은_건드리지_않는다() {
		when(hashes.entries("mapping:client-1:chat-1")).thenReturn(Map.of(
				"iv", "aXY=", "ciphertext", "Y2lwaGVydGV4dA==", "updatedAt", "2026-10-21T00:00:00Z"));

		assertThat(repository.findByClientIdAndChatId("client-1", "chat-1"))
				.contains(new EncryptedMapping("aXY=", "Y2lwaGVydGV4dA=="));

		verify(redisTemplate).opsForHash();
		verify(hashes).entries("mapping:client-1:chat-1");
		verifyNoMoreInteractions(redisTemplate, hashes, cipher, codec);
	}

	@Test
	void 없거나_만료된_매핑은_빈_결과를_반환한다() {
		when(hashes.entries("mapping:client-1:chat-1")).thenReturn(Map.of());

		assertThat(repository.findByClientIdAndChatId("client-1", "chat-1")).isEmpty();
	}

	@Test
	void 다른_클라이언트의_같은_대화는_별도_키로_조회한다() {
		when(hashes.entries("mapping:client-2:chat-1")).thenReturn(Map.of());

		assertThat(repository.findByClientIdAndChatId("client-2", "chat-1")).isEmpty();
		verify(hashes).entries("mapping:client-2:chat-1");
		verifyNoMoreInteractions(hashes);
	}

	@Test
	void 저장소_오류를_매핑_없음으로_숨기지_않는다() {
		when(hashes.entries("mapping:client-1:chat-1"))
				.thenThrow(new IllegalStateException("storage unavailable"));

		assertThatThrownBy(() -> repository.findByClientIdAndChatId("client-1", "chat-1"))
				.isInstanceOf(IllegalStateException.class);
	}
}
