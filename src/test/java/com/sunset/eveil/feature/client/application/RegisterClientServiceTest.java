package com.sunset.eveil.feature.client.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunset.eveil.feature.client.domain.Client;
import com.sunset.eveil.feature.client.domain.ClientRepository;
import com.sunset.eveil.feature.client.exception.ClientAlreadyExistsException;
import com.sunset.eveil.global.crypto.AesKeyGenerator;
import com.sunset.eveil.global.crypto.KeyWrapper;
import com.sunset.eveil.global.crypto.Sha256Hasher;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegisterClientServiceTest {

	private static final String CLIENT_ID = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d";

	private final InMemoryClientRepository repository = new InMemoryClientRepository();
	private final KeyWrapper keyWrapper = new KeyWrapper();
	private final Sha256Hasher hasher = new Sha256Hasher();
	private final RegisterClientService service =
			new RegisterClientService(repository, new AesKeyGenerator(), keyWrapper, hasher);

	@Test
	void 등록하면_authKey_해시와_KEK로_감싼_AES_키만_저장된다() {
		byte[] authKey = "auth-key".getBytes(StandardCharsets.UTF_8);
		byte[] kek = randomKek();
		byte[] kekCopy = kek.clone();
		byte[] authKeyCopy = authKey.clone();

		service.execute(new RegisterClientCommand(CLIENT_ID, authKey, kek));

		Client saved = repository.findById(CLIENT_ID).orElseThrow();
		assertThat(saved.getAuthKeyHash()).hasSize(32);
		assertThat(hasher.matches(authKeyCopy, saved.getAuthKeyHash())).isTrue();
		assertThat(saved.getWrappedAesKey()).hasSize(40);
		assertThat(keyWrapper.unwrap(kekCopy, saved.getWrappedAesKey()).getEncoded()).hasSize(32);
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void 처리_후_kek와_authKey_원본은_0으로_덮어써진다() {
		byte[] authKey = "auth-key".getBytes(StandardCharsets.UTF_8);
		byte[] kek = randomKek();

		service.execute(new RegisterClientCommand(CLIENT_ID, authKey, kek));

		assertThat(kek).containsOnly(0);
		assertThat(authKey).containsOnly(0);
	}

	@Test
	void 이미_등록된_clientId면_ClientAlreadyExistsException이_난다() {
		service.execute(new RegisterClientCommand(CLIENT_ID, "a".getBytes(StandardCharsets.UTF_8), randomKek()));

		assertThatThrownBy(() -> service.execute(
				new RegisterClientCommand(CLIENT_ID, "b".getBytes(StandardCharsets.UTF_8), randomKek())))
				.isInstanceOf(ClientAlreadyExistsException.class);
		assertThat(repository.size()).isEqualTo(1);
	}

	private byte[] randomKek() {
		byte[] kek = new byte[32];
		new SecureRandom().nextBytes(kek);
		return kek;
	}

	static class InMemoryClientRepository implements ClientRepository {

		private final Map<String, Client> store = new HashMap<>();

		@Override
		public boolean exists(String clientId) {
			return store.containsKey(clientId);
		}

		@Override
		public void save(Client client) {
			store.put(client.getClientId(), client);
		}

		@Override
		public Optional<Client> findById(String clientId) {
			return Optional.ofNullable(store.get(clientId));
		}

		int size() {
			return store.size();
		}
	}
}
