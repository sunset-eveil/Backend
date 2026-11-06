package com.sunset.eveil.feature.masking.infrastructure;

import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.feature.masking.domain.Detector;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * AI의 코드 포인트 위치·유형을 기존 탐지 결과로 변환한다.
 * 기본 탐지기이며 demo 모드에서는 등록하지 않는다.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.masking.detector", havingValue = "ai", matchIfMissing = true)
public class AiDetector implements Detector {

	private final AiInferenceClient client;

	public AiDetector(AiInferenceClient client) {
		this.client = Objects.requireNonNull(client);
	}

	@Override
	public List<DetectedEntity> detect(String text, Set<MaskingType> excludeTypes) {
		Set<MaskingType> excluded = excludeTypes == null ? Set.of() : excludeTypes;
		int length = text.codePointCount(0, text.length());
		return client.detect(text).stream()
				.map(span -> {
					if (span.end() > length) {
						throw new IllegalArgumentException("Invalid AI detection span");
					}
					MaskingType type = switch (span.type()) {
						case "NAME" -> MaskingType.NAME;
						case "PHONE" -> MaskingType.PHONE;
						case "ADDRESS" -> MaskingType.ADDRESS;
						case "ACCOUNT", "CARD" -> MaskingType.CARD_ACCOUNT;
						case "RRN" -> MaskingType.RRN;
						case "EMAIL" -> MaskingType.EMAIL;
						case "AMOUNT" -> MaskingType.AMOUNT;
						default -> throw new IllegalArgumentException("Invalid AI detection type");
					};
					int start = text.offsetByCodePoints(0, span.start());
					int end = text.offsetByCodePoints(0, span.end());
					return new DetectedEntity(text.substring(start, end), type);
				})
				.filter(entity -> !excluded.contains(entity.type()))
				.distinct()
				.sorted(Comparator.comparingInt((DetectedEntity entity) -> entity.value().length()).reversed())
				.toList();
	}
}
