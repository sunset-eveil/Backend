package com.sunset.eveil.feature.masking.infrastructure;

import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.feature.masking.domain.Detector;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 시연용 고정 값 탐지기. 아래 FIXED_VALUES에 있는 값이 텍스트에 포함되어 있으면 그대로 돌려준다.
 * Redis·외부 조회 없이 순수 메모리로 동작한다. 영상 시나리오에 맞춰 값만 바꿔 끼우면 된다.
 * CompositeDetector에서 정규식 탐지기보다 먼저 오므로, 같은 값에 대해서는 이 목록의 유형이 우선한다.
 */
@Component
@Order(1)
public class FixedValueDetector implements Detector {

	// ── 시연용 고정 값 목록 (여기만 수정) ─────────────────────────────
	static final Map<String, MaskingType> FIXED_VALUES = Map.ofEntries(
			Map.entry("홍길동", MaskingType.NAME),
			Map.entry("김철수", MaskingType.NAME),
			Map.entry("서울시 강남구 테헤란로 123", MaskingType.ADDRESS),
			Map.entry("500만원", MaskingType.AMOUNT)
	);
	// ────────────────────────────────────────────────────────────────

	@Override
	public List<DetectedEntity> detect(String text, Set<MaskingType> excludeTypes) {
		Set<MaskingType> excluded = excludeTypes == null ? Set.of() : excludeTypes;
		return FIXED_VALUES.entrySet().stream()
				.filter(entry -> !excluded.contains(entry.getValue()))
				.filter(entry -> text.contains(entry.getKey()))
				.map(entry -> new DetectedEntity(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparingInt((DetectedEntity entity) -> entity.value().length()).reversed())
				.toList();
	}
}
