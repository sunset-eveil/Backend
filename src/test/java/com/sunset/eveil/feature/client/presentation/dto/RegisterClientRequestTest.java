package com.sunset.eveil.feature.client.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunset.eveil.feature.client.application.RegisterClientCommand;
import com.sunset.eveil.feature.client.exception.InvalidClientRequestException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class RegisterClientRequestTest {

	private static final String CLIENT_ID = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
	private static final String KEK_32_BYTES = Base64.getEncoder().encodeToString(new byte[32]);
	private static final String AUTH_KEY = Base64.getEncoder().encodeToString("auth".getBytes());

	@Test
	void 유효한_Base64와_32바이트_kek면_Command로_바뀐다() {
		RegisterClientCommand command = new RegisterClientRequest(CLIENT_ID, AUTH_KEY, KEK_32_BYTES).toCommand();

		assertThat(command.clientId()).isEqualTo(CLIENT_ID);
		assertThat(command.authKey()).isEqualTo("auth".getBytes());
		assertThat(command.kek()).hasSize(32);
	}

	@Test
	void kek가_32바이트가_아니면_InvalidClientRequestException이_난다() {
		String kek16 = Base64.getEncoder().encodeToString(new byte[16]);

		assertThatThrownBy(() -> new RegisterClientRequest(CLIENT_ID, AUTH_KEY, kek16).toCommand())
				.isInstanceOf(InvalidClientRequestException.class);
	}

	@Test
	void Base64가_아니면_InvalidClientRequestException이_난다() {
		assertThatThrownBy(() -> new RegisterClientRequest(CLIENT_ID, "not-base64!!", KEK_32_BYTES).toCommand())
				.isInstanceOf(InvalidClientRequestException.class);
	}
}
