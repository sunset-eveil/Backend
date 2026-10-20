package com.sunset.eveil.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class Sha256HasherTest {

	private final Sha256Hasher hasher = new Sha256Hasher();

	@Test
	void 해시는_32바이트이고_같은_입력이면_같다() {
		byte[] input = "auth-key".getBytes(StandardCharsets.UTF_8);

		assertThat(hasher.hash(input)).hasSize(32).isEqualTo(hasher.hash(input));
	}

	@Test
	void 같은_입력은_일치하고_다른_입력은_불일치한다() {
		byte[] stored = hasher.hash("auth-key".getBytes(StandardCharsets.UTF_8));

		assertThat(hasher.matches("auth-key".getBytes(StandardCharsets.UTF_8), stored)).isTrue();
		assertThat(hasher.matches("auth-kex".getBytes(StandardCharsets.UTF_8), stored)).isFalse();
	}
}
