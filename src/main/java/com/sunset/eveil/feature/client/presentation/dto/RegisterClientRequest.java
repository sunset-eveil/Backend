package com.sunset.eveil.feature.client.presentation.dto;

import com.sunset.eveil.feature.client.application.RegisterClientCommand;
import com.sunset.eveil.feature.client.exception.InvalidClientRequestException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Base64;

/**
 * POST /api/clients 요청 body. authKey·kek는 Base64 문자열이며, kek는 풀었을 때 32바이트여야 한다.
 */
public record RegisterClientRequest(
		@NotBlank
		@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
		String clientId,
		@NotBlank String authKey,
		@NotBlank String kek
) {

	public RegisterClientCommand toCommand() {
		byte[] authKeyBytes = decode(authKey);
		byte[] kekBytes = decode(kek);
		if (authKeyBytes.length == 0 || kekBytes.length != RegisterClientCommand.KEK_LENGTH_BYTES) {
			throw new InvalidClientRequestException();
		}
		return new RegisterClientCommand(clientId, authKeyBytes, kekBytes);
	}

	private static byte[] decode(String base64) {
		try {
			return Base64.getDecoder().decode(base64);
		} catch (IllegalArgumentException e) {
			throw new InvalidClientRequestException();
		}
	}
}
