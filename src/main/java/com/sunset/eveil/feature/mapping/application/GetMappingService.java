package com.sunset.eveil.feature.mapping.application;

import com.sunset.eveil.feature.mapping.domain.EncryptedMapping;
import com.sunset.eveil.feature.mapping.domain.MappingRepository;
import com.sunset.eveil.feature.mapping.exception.MappingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMappingService {

	private final MappingRepository mappingRepository;

	public EncryptedMapping execute(String clientId, String chatId) {
		return mappingRepository.findByClientIdAndChatId(clientId, chatId)
				.orElseThrow(MappingNotFoundException::new);
	}
}
