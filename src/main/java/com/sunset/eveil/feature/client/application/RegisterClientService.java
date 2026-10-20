package com.sunset.eveil.feature.client.application;

import com.sunset.eveil.feature.client.domain.Client;
import com.sunset.eveil.feature.client.domain.ClientRepository;
import com.sunset.eveil.feature.client.exception.ClientAlreadyExistsException;
import com.sunset.eveil.global.crypto.AesKeyGenerator;
import com.sunset.eveil.global.crypto.KeyWrapper;
import com.sunset.eveil.global.crypto.Sha256Hasher;
import java.time.Instant;
import java.util.Arrays;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 클라이언트 등록 (CLAUDE.md 5번 "클라이언트 등록").
 * kek, authKey, AES 키 원본은 이 메서드의 지역 범위에서만 존재하고, 저장되는 것은 해시와 감싼 키뿐이다.
 */
@Service
@RequiredArgsConstructor
public class RegisterClientService {

	private final ClientRepository clientRepository;
	private final AesKeyGenerator aesKeyGenerator;
	private final KeyWrapper keyWrapper;
	private final Sha256Hasher sha256Hasher;

	public void execute(RegisterClientCommand command) {
		if (clientRepository.exists(command.clientId())) {
			throw new ClientAlreadyExistsException();
		}
		try {
			SecretKey aesKey = aesKeyGenerator.generate();
			byte[] wrappedAesKey = keyWrapper.wrap(command.kek(), aesKey);
			byte[] authKeyHash = sha256Hasher.hash(command.authKey());

			clientRepository.save(Client.register(command.clientId(), authKeyHash, wrappedAesKey, Instant.now()));
		} finally {
			// 원본 폐기: 요청 객체가 참조를 들고 있더라도 메모리에 값이 남지 않게 한다.
			Arrays.fill(command.kek(), (byte) 0);
			Arrays.fill(command.authKey(), (byte) 0);
		}
	}
}
