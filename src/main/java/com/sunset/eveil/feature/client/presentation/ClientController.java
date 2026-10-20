package com.sunset.eveil.feature.client.presentation;

import com.sunset.eveil.feature.client.application.RegisterClientService;
import com.sunset.eveil.feature.client.presentation.dto.RegisterClientRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

	private final RegisterClientService registerClientService;

	/** docs/API.md 1번. 성공 시 201, body 없음. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void register(@Valid @RequestBody RegisterClientRequest request) {
		registerClientService.execute(request.toCommand());
	}
}
