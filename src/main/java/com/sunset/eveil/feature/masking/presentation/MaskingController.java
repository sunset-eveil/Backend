package com.sunset.eveil.feature.masking.presentation;

import com.sunset.eveil.feature.masking.application.MaskTextService;
import com.sunset.eveil.feature.masking.presentation.dto.MaskTextRequest;
import com.sunset.eveil.global.auth.AuthHeaders;
import com.sunset.eveil.global.auth.AuthenticatedClient;
import com.sunset.eveil.global.auth.Kek;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/{chatId}/maskings")
@RequiredArgsConstructor
public class MaskingController {

	private final MaskTextService maskTextService;

	/** docs/API.md 2번. 성공 시 204, body 없음. 결과는 GET 매핑 조회로 받는다. */
	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void mask(
			AuthenticatedClient client,
			@RequestHeader(AuthHeaders.KEK) String kekBase64,
			@PathVariable UUID chatId,
			@RequestBody MaskTextRequest request
	) {
		maskTextService.execute(request.toCommand(client, Kek.fromBase64(kekBase64), chatId.toString()));
	}
}
