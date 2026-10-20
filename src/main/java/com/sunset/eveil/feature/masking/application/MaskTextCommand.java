package com.sunset.eveil.feature.masking.application;

import com.sunset.eveil.global.mask.MaskingType;
import java.util.Set;

/**
 * 마스킹 유스케이스 입력. kek는 원본 바이트이며 서비스가 사용 후 0으로 덮어쓴다.
 */
public record MaskTextCommand(
		String clientId,
		String chatId,
		byte[] wrappedAesKey,
		byte[] kek,
		String text,
		Set<MaskingType> excludeTypes
) {

	public MaskTextCommand {
		if (clientId == null || clientId.isBlank()) {
			throw new IllegalArgumentException("clientId must not be blank");
		}
		if (chatId == null || chatId.isBlank()) {
			throw new IllegalArgumentException("chatId must not be blank");
		}
		if (wrappedAesKey == null || wrappedAesKey.length == 0) {
			throw new IllegalArgumentException("wrappedAesKey must not be empty");
		}
		if (kek == null || kek.length == 0) {
			throw new IllegalArgumentException("kek must not be empty");
		}
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("text must not be blank");
		}
		excludeTypes = excludeTypes == null ? Set.of() : Set.copyOf(excludeTypes);
	}
}
