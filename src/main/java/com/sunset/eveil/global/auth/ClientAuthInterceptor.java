package com.sunset.eveil.global.auth;

import com.sunset.eveil.global.crypto.Sha256Hasher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * X-Client-Id + X-Auth-Key 공통 인증 (CLAUDE.md 5번 1~2단계).
 * - 헤더 누락, 미등록, 해시 불일치 → 401
 * - 헤더 형식 오류 → 400
 * authKey 원본은 이 메서드의 지역 변수로만 다루고 어디에도 남기지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ClientAuthInterceptor implements HandlerInterceptor {

	private static final Pattern UUID_PATTERN =
			Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private final ClientCredentialsPort clientCredentialsPort;
	private final Sha256Hasher sha256Hasher;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String clientId = request.getHeader(AuthHeaders.CLIENT_ID);
		String authKeyBase64 = request.getHeader(AuthHeaders.AUTH_KEY);
		if (clientId == null || authKeyBase64 == null) {
			throw new ClientAuthenticationException();
		}
		if (!UUID_PATTERN.matcher(clientId).matches()) {
			throw new InvalidAuthHeaderException();
		}
		byte[] authKey = decodeAuthKey(authKeyBase64);

		ClientCredentials credentials = clientCredentialsPort.findCredentials(clientId)
				.orElseThrow(ClientAuthenticationException::new);
		if (!sha256Hasher.matches(authKey, credentials.authKeyHash())) {
			throw new ClientAuthenticationException();
		}

		request.setAttribute(AuthenticatedClient.REQUEST_ATTRIBUTE,
				new AuthenticatedClient(clientId, credentials.wrappedAesKey()));
		return true;
	}

	private byte[] decodeAuthKey(String authKeyBase64) {
		try {
			byte[] authKey = Base64.getDecoder().decode(authKeyBase64);
			if (authKey.length == 0) {
				throw new InvalidAuthHeaderException();
			}
			return authKey;
		} catch (IllegalArgumentException e) {
			throw new InvalidAuthHeaderException();
		}
	}
}
