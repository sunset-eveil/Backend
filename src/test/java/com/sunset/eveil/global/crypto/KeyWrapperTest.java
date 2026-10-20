package com.sunset.eveil.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class KeyWrapperTest {

	private final KeyWrapper keyWrapper = new KeyWrapper();
	private final AesKeyGenerator keyGenerator = new AesKeyGenerator();

	@Test
	void AES256_키를_감싸면_40바이트가_된다() {
		byte[] wrapped = keyWrapper.wrap(randomKek(), keyGenerator.generate());

		assertThat(wrapped).hasSize(40);
	}

	@Test
	void 같은_KEK로_풀면_원래_키가_나온다() {
		byte[] kek = randomKek();
		SecretKey aesKey = keyGenerator.generate();

		SecretKey unwrapped = keyWrapper.unwrap(kek, keyWrapper.wrap(kek, aesKey));

		assertThat(unwrapped.getEncoded()).isEqualTo(aesKey.getEncoded());
	}

	@Test
	void 다른_KEK로_풀면_CryptoException이_나고_원인은_메시지에_없다() {
		byte[] wrapped = keyWrapper.wrap(randomKek(), keyGenerator.generate());

		assertThatThrownBy(() -> keyWrapper.unwrap(randomKek(), wrapped))
				.isInstanceOf(CryptoException.class)
				.hasMessage(CryptoException.MESSAGE)
				.hasNoCause();
	}

	@Test
	void KEK_길이가_32바이트가_아니면_CryptoException이_난다() {
		byte[] wrapped = keyWrapper.wrap(randomKek(), keyGenerator.generate());

		assertThatThrownBy(() -> keyWrapper.unwrap(new byte[16], wrapped))
				.isInstanceOf(CryptoException.class);
	}

	private byte[] randomKek() {
		byte[] kek = new byte[32];
		new SecureRandom().nextBytes(kek);
		return kek;
	}
}
