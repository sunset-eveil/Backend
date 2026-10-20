package com.sunset.eveil.feature.masking.presentation.dto;

import com.sunset.eveil.global.mask.MaskingType;
import com.sunset.eveil.feature.masking.application.MaskTextCommand;
import com.sunset.eveil.feature.masking.exception.EmptyMaskingTextException;
import com.sunset.eveil.global.auth.AuthenticatedClient;
import com.sunset.eveil.global.auth.Kek;
import java.util.Set;

/**
 * POST /api/chats/{chatId}/maskings 요청 body.
 * excludeTypes에 정의되지 않은 유형이 오면 역직렬화 단계에서 400 처리된다.
 */
public record MaskTextRequest(String text, Set<MaskingType> excludeTypes) {

	public MaskTextCommand toCommand(AuthenticatedClient client, Kek kek, String chatId) {
		if (text == null || text.isBlank()) {
			throw new EmptyMaskingTextException();
		}
		return new MaskTextCommand(client.clientId(), chatId, client.wrappedAesKey(), kek.bytes(), text, excludeTypes);
	}
}
