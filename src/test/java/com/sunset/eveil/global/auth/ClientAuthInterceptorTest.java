package com.sunset.eveil.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunset.eveil.global.crypto.Sha256Hasher;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ClientAuthInterceptorTest {

	private static final String CLIENT_ID = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
	private static final byte[] AUTH_KEY = "auth-key".getBytes(StandardCharsets.UTF_8);
	private static final byte[] WRAPPED_AES_KEY = new byte[40];

	private final Sha256Hasher hasher = new Sha256Hasher();
	private final ClientCredentialsPort port = clientId -> CLIENT_ID.equals(clientId)
			? Optional.of(new ClientCredentials(CLIENT_ID, hasher.hash(AUTH_KEY), WRAPPED_AES_KEY))
			: Optional.empty();
	private final ClientAuthInterceptor interceptor = new ClientAuthInterceptor(port, hasher);

	@Test
	void 올바른_헤더면_통과하고_AuthenticatedClient를_요청에_넣는다() {
		MockHttpServletRequest request = requestWith(CLIENT_ID, base64(AUTH_KEY));

		boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

		assertThat(result).isTrue();
		AuthenticatedClient client = (AuthenticatedClient) request.getAttribute(AuthenticatedClient.REQUEST_ATTRIBUTE);
		assertThat(client.clientId()).isEqualTo(CLIENT_ID);
		assertThat(client.wrappedAesKey()).isEqualTo(WRAPPED_AES_KEY);
	}

	@Test
	void 헤더가_없으면_401이다() {
		assertThatThrownBy(() -> interceptor.preHandle(
				new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()))
				.isInstanceOf(ClientAuthenticationException.class);
	}

	@Test
	void 미등록_clientId면_401이다() {
		MockHttpServletRequest request = requestWith("ffffffff-ffff-4fff-8fff-ffffffffffff", base64(AUTH_KEY));

		assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
				.isInstanceOf(ClientAuthenticationException.class);
	}

	@Test
	void authKey가_다르면_401이다() {
		MockHttpServletRequest request = requestWith(CLIENT_ID, base64("wrong".getBytes(StandardCharsets.UTF_8)));

		assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
				.isInstanceOf(ClientAuthenticationException.class);
	}

	@Test
	void clientId가_UUID_형식이_아니면_400이다() {
		MockHttpServletRequest request = requestWith("not-a-uuid", base64(AUTH_KEY));

		assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
				.isInstanceOf(InvalidAuthHeaderException.class);
	}

	@Test
	void authKey가_Base64가_아니면_400이다() {
		MockHttpServletRequest request = requestWith(CLIENT_ID, "not-base64!!");

		assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
				.isInstanceOf(InvalidAuthHeaderException.class);
	}

	private MockHttpServletRequest requestWith(String clientId, String authKeyBase64) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(AuthHeaders.CLIENT_ID, clientId);
		request.addHeader(AuthHeaders.AUTH_KEY, authKeyBase64);
		return request;
	}

	private static String base64(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}
}
