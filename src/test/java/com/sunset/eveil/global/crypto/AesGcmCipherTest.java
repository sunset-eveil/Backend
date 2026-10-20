package com.sunset.eveil.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class AesGcmCipherTest {

	private final AesGcmCipher cipher = new AesGcmCipher();
	private final AesKeyGenerator keyGenerator = new AesKeyGenerator();

	@Test
	void 암호화한_것을_같은_키로_복호화하면_원문이_나온다() {
		SecretKey key = keyGenerator.generate();
		byte[] plaintext = "{\"mappings\":[]}".getBytes(StandardCharsets.UTF_8);

		EncryptedPayload payload = cipher.encrypt(key, plaintext);

		assertThat(cipher.decrypt(key, payload)).isEqualTo(plaintext);
	}

	@Test
	void 같은_평문을_두_번_암호화하면_IV와_암호문이_모두_다르다() {
		SecretKey key = keyGenerator.generate();
		byte[] plaintext = "same".getBytes(StandardCharsets.UTF_8);

		EncryptedPayload first = cipher.encrypt(key, plaintext);
		EncryptedPayload second = cipher.encrypt(key, plaintext);

		assertThat(first.iv()).hasSize(12).isNotEqualTo(second.iv());
		assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
	}

	@Test
	void 암호문을_변조하면_CryptoException이_난다() {
		SecretKey key = keyGenerator.generate();
		EncryptedPayload payload = cipher.encrypt(key, "hello".getBytes(StandardCharsets.UTF_8));
		byte[] tampered = payload.ciphertext().clone();
		tampered[0] ^= 0x01;

		assertThatThrownBy(() -> cipher.decrypt(key, new EncryptedPayload(payload.iv(), tampered)))
				.isInstanceOf(CryptoException.class);
	}

	@Test
	void 다른_키로_복호화하면_CryptoException이_나고_원인은_메시지에_없다() {
		EncryptedPayload payload = cipher.encrypt(keyGenerator.generate(), "hello".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> cipher.decrypt(keyGenerator.generate(), payload))
				.isInstanceOf(CryptoException.class)
				.hasMessage(CryptoException.MESSAGE)
				.hasNoCause();
	}
}
