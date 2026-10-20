package com.sunset.eveil.feature.client.infrastructure;

import com.sunset.eveil.feature.client.domain.ClientRepository;
import com.sunset.eveil.global.auth.ClientCredentials;
import com.sunset.eveil.global.auth.ClientCredentialsPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * global/auth의 ClientCredentialsPort 구현. 인터셉터가 feature/client를 모른 채 인증 정보를 얻게 한다.
 */
@Component
@RequiredArgsConstructor
public class ClientCredentialsAdapter implements ClientCredentialsPort {

	private final ClientRepository clientRepository;

	@Override
	public Optional<ClientCredentials> findCredentials(String clientId) {
		return clientRepository.findById(clientId)
				.map(client -> new ClientCredentials(
						client.getClientId(), client.getAuthKeyHash(), client.getWrappedAesKey()));
	}
}
