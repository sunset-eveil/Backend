package com.sunset.eveil.feature.mapping.presentation;

import com.sunset.eveil.feature.mapping.application.GetMappingService;
import com.sunset.eveil.feature.mapping.domain.EncryptedMapping;
import com.sunset.eveil.feature.mapping.presentation.dto.GetMappingResponse;
import com.sunset.eveil.global.auth.AuthenticatedClient;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/{chatId}/mappings")
@RequiredArgsConstructor
public class MappingController {

	private final GetMappingService getMappingService;

	@GetMapping
	public ResponseEntity<GetMappingResponse> getMapping(
			@PathVariable UUID chatId, AuthenticatedClient client) {
		EncryptedMapping mapping = getMappingService.execute(client.clientId(), chatId.toString());
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(new GetMappingResponse(Base64.getEncoder().encodeToString(client.wrappedAesKey()),
						mapping.iv(), mapping.ciphertext()));
	}
}
