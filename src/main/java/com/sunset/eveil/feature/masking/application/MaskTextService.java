package com.sunset.eveil.feature.masking.application;

import com.sunset.eveil.feature.mapping.domain.MappingRepository;
import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.feature.masking.domain.Detector;
import com.sunset.eveil.feature.masking.domain.OriginalText;
import com.sunset.eveil.global.crypto.KeyWrapper;
import com.sunset.eveil.global.lock.ChatLockManager;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 마스킹 요청 (CLAUDE.md 5번 "매핑을 수정하는 API"). 인증(1~2단계)은 인터셉터가 끝낸 뒤 들어온다.
 * 3. KEK로 AES 키 풀기 → 4. chatId 락 → 5. 매핑 조회·복호화 → 6. 탐지·배정 → 7~8. 암호화·저장 → 9. 락 해제 → 10. 원본 폐기
 * KEK·AES 키·원문은 이 메서드의 지역 범위에서만 존재한다.
 */
@Service
@RequiredArgsConstructor
public class MaskTextService {

	private final MappingRepository mappingRepository;
	private final Detector detector;
	private final KeyWrapper keyWrapper;
	private final ChatLockManager chatLockManager;

	public void execute(MaskTextCommand command) {
		try {
			SecretKey aesKey = keyWrapper.unwrap(command.kek(), command.wrappedAesKey());
			OriginalText originalText = new OriginalText(command.text());

			chatLockManager.withLock(command.clientId(), command.chatId(), () -> {
				MappingTable table = mappingRepository
						.find(command.clientId(), command.chatId(), aesKey)
						.orElseGet(MappingTable::empty);

				List<DetectedEntity> entities =
						originalText.keepPresent(detector.detect(command.text(), command.excludeTypes()));
				for (DetectedEntity entity : entities) {
					table.assign(entity.value(), entity.type());
				}

				mappingRepository.save(command.clientId(), command.chatId(), table, aesKey);
				return null;
			});
		} finally {
			Arrays.fill(command.kek(), (byte) 0);
		}
	}
}
