package com.sunset.eveil.feature.masking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunset.eveil.feature.mapping.domain.MappingRepository;
import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.feature.mapping.domain.Mapping;
import com.sunset.eveil.global.mask.MaskingType;
import com.sunset.eveil.feature.masking.infrastructure.RegexDetector;
import com.sunset.eveil.global.crypto.AesKeyGenerator;
import com.sunset.eveil.global.crypto.CryptoException;
import com.sunset.eveil.global.crypto.KeyWrapper;
import com.sunset.eveil.global.lock.ChatLockManager;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class MaskTextServiceTest {

	private static final String CLIENT_ID = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
	private static final String CHAT_ID = "11111111-2222-4333-8444-555555555555";

	private final InMemoryMappingRepository repository = new InMemoryMappingRepository();
	private final KeyWrapper keyWrapper = new KeyWrapper();
	private final MaskTextService service =
			new MaskTextService(repository, new RegexDetector(), keyWrapper, new ChatLockManager());

	private final byte[] kek = randomBytes(32);
	private final byte[] wrappedAesKey = keyWrapper.wrap(kek, new AesKeyGenerator().generate());

	@Test
	void 첫_요청이면_매핑을_새로_만들고_탐지된_값에_코드를_배정한다() {
		service.execute(command("전화 010-1234-5678, 메일 a@b.com", Set.of()));

		MappingTable saved = repository.stored.get(CHAT_ID);
		assertThat(saved.mappings()).extracting(Mapping::value, mapping -> mapping.maskCode().text())
				.containsExactlyInAnyOrder(
						org.assertj.core.groups.Tuple.tuple("010-1234-5678", "PHONE1"),
						org.assertj.core.groups.Tuple.tuple("a@b.com", "EMAIL1"));
		assertThat(repository.savedWithKeys).hasSize(1);
	}

	@Test
	void 두_번째_요청은_기존_코드를_유지하고_새_값만_다음_번호를_받는다() {
		service.execute(command("전화 010-1234-5678", Set.of()));
		service.execute(command("다시 010-1234-5678 그리고 010-9999-8888", Set.of()));

		MappingTable saved = repository.stored.get(CHAT_ID);
		assertThat(saved.findByValue("010-1234-5678")).map(mapping -> mapping.maskCode().text()).contains("PHONE1");
		assertThat(saved.findByValue("010-9999-8888")).map(mapping -> mapping.maskCode().text()).contains("PHONE2");
		assertThat(saved.counters()).containsEntry(MaskingType.PHONE, 2);
	}

	@Test
	void excludeTypes의_유형은_매핑에_반영되지_않는다() {
		service.execute(command("전화 010-1234-5678, 메일 a@b.com", Set.of(MaskingType.PHONE)));

		assertThat(repository.stored.get(CHAT_ID).mappings()).extracting(Mapping::type)
				.containsExactly(MaskingType.EMAIL);
	}

	@Test
	void 탐지된_것이_없어도_빈_매핑을_저장한다() {
		service.execute(command("개인정보 없는 문장", Set.of()));

		assertThat(repository.stored.get(CHAT_ID).isEmpty()).isTrue();
	}

	@Test
	void 틀린_KEK면_CryptoException이_나고_저장하지_않는다() {
		byte[] wrongKek = randomBytes(32);

		assertThatThrownBy(() -> service.execute(new MaskTextCommand(
				CLIENT_ID, CHAT_ID, wrappedAesKey, wrongKek, "010-1234-5678", Set.of())))
				.isInstanceOf(CryptoException.class);
		assertThat(repository.stored).isEmpty();
	}

	@Test
	void 처리_후_KEK_원본은_0으로_덮어써진다() {
		byte[] kekCopy = kek.clone();
		MaskTextCommand command = new MaskTextCommand(CLIENT_ID, CHAT_ID, wrappedAesKey, kekCopy, "a@b.com", Set.of());

		service.execute(command);

		assertThat(kekCopy).containsOnly(0);
	}

	private MaskTextCommand command(String text, Set<MaskingType> excludeTypes) {
		return new MaskTextCommand(CLIENT_ID, CHAT_ID, wrappedAesKey, kek.clone(), text, excludeTypes);
	}

	private static byte[] randomBytes(int length) {
		byte[] bytes = new byte[length];
		new SecureRandom().nextBytes(bytes);
		return bytes;
	}

	/** 암호화 없이 평문 테이블을 들고 있는 가짜 저장소. 저장 시 넘어온 AES 키를 기록한다. */
	static class InMemoryMappingRepository implements MappingRepository {

		@Override
		public Optional<com.sunset.eveil.feature.mapping.domain.EncryptedMapping> findByClientIdAndChatId(
				String clientId, String chatId) {
			throw new UnsupportedOperationException("Masking tests do not read encrypted mappings");
		}

		final Map<String, MappingTable> stored = new HashMap<>();
		final java.util.List<SecretKey> savedWithKeys = new java.util.ArrayList<>();

		@Override
		public Optional<MappingTable> find(String clientId, String chatId, SecretKey aesKey) {
			return Optional.ofNullable(stored.get(chatId));
		}

		@Override
		public void save(String clientId, String chatId, MappingTable table, SecretKey aesKey) {
			stored.put(chatId, table);
			savedWithKeys.add(aesKey);
		}
	}
}
