package com.sunset.eveil.feature.mapping.application;

import com.sunset.eveil.feature.mapping.domain.MappingRepository;
import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.feature.mapping.domain.MaskCode;
import com.sunset.eveil.feature.mapping.exception.UnmaskingNotFoundException;
import com.sunset.eveil.global.auth.AuthenticatedClient;
import com.sunset.eveil.global.crypto.KeyWrapper;
import com.sunset.eveil.global.lock.ChatLockManager;
import java.util.Arrays;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnmaskMappingService {

	private final MappingRepository mappingRepository;
	private final KeyWrapper keyWrapper;
	private final ChatLockManager chatLockManager;

	public void execute(AuthenticatedClient client, String chatId, MaskCode maskCode, byte[] kek) {
		try {
			SecretKey aesKey = keyWrapper.unwrap(kek, client.wrappedAesKey());
			chatLockManager.withLock(client.clientId(), chatId, () -> {
				MappingTable table = mappingRepository.find(client.clientId(), chatId, aesKey)
						.orElseThrow(UnmaskingNotFoundException::new);
				if (!table.remove(maskCode)) {
					throw new UnmaskingNotFoundException();
				}
				mappingRepository.save(client.clientId(), chatId, table, aesKey);
				return null;
			});
		} finally {
			Arrays.fill(kek, (byte) 0);
		}
	}
}
