package com.sunset.eveil.feature.mapping.application;

import com.sunset.eveil.feature.mapping.domain.MappingRepository;
import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.global.auth.AuthenticatedClient;
import com.sunset.eveil.global.crypto.KeyWrapper;
import com.sunset.eveil.global.lock.ChatLockManager;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.Arrays;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateMappingTypeService {

	private final MappingRepository mappingRepository;
	private final KeyWrapper keyWrapper;
	private final ChatLockManager chatLockManager;

	public void execute(AuthenticatedClient client, String chatId, String value, MaskingType type, byte[] kek) {
		try {
			SecretKey aesKey = keyWrapper.unwrap(kek, client.wrappedAesKey());
			chatLockManager.withLock(client.clientId(), chatId, () -> {
				MappingTable table = mappingRepository.find(client.clientId(), chatId, aesKey)
						.orElseGet(MappingTable::empty);
				table.changeType(value, type);
				mappingRepository.save(client.clientId(), chatId, table, aesKey);
				return null;
			});
		} finally {
			Arrays.fill(kek, (byte) 0);
		}
	}
}
