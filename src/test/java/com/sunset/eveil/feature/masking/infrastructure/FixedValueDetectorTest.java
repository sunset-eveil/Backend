package com.sunset.eveil.feature.masking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FixedValueDetectorTest {

	private final FixedValueDetector detector = new FixedValueDetector();

	@Test
	void 텍스트에_포함된_고정_값을_유형과_함께_탐지한다() {
		List<DetectedEntity> result = detector.detect(
				"홍길동이 서울시 강남구 테헤란로 123에서 500만원을 송금했다", Set.of());

		assertThat(result).containsExactlyInAnyOrder(
				new DetectedEntity("홍길동", MaskingType.NAME),
				new DetectedEntity("서울시 강남구 테헤란로 123", MaskingType.ADDRESS),
				new DetectedEntity("500만원", MaskingType.AMOUNT));
	}

	@Test
	void excludeTypes의_유형은_탐지하지_않는다() {
		List<DetectedEntity> result = detector.detect("홍길동이 500만원을 보냈다", Set.of(MaskingType.AMOUNT));

		assertThat(result).containsExactly(new DetectedEntity("홍길동", MaskingType.NAME));
	}

	@Test
	void 목록에_없는_값은_탐지하지_않는다() {
		assertThat(detector.detect("홍길다가 100만원을 보냈다", Set.of())).isEmpty();
	}

	@Test
	void 긴_값부터_돌려준다() {
		List<DetectedEntity> result = detector.detect("홍길동, 서울시 강남구 테헤란로 123", Set.of());

		assertThat(result).extracting(DetectedEntity::value)
				.containsExactly("서울시 강남구 테헤란로 123", "홍길동");
	}
}
