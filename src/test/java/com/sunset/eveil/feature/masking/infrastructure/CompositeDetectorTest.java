package com.sunset.eveil.feature.masking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.feature.masking.domain.Detector;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompositeDetectorTest {

	@Test
	void 고정값과_정규식_결과를_합친다() {
		CompositeDetector composite = new CompositeDetector(List.of(new FixedValueDetector(), new RegexDetector()));

		List<DetectedEntity> result = composite.detect("안녕하세요 홍길동입니다. 연락처는 010-1234-5678", Set.of());

		assertThat(result).containsExactly(
				new DetectedEntity("010-1234-5678", MaskingType.PHONE),
				new DetectedEntity("홍길동", MaskingType.NAME));
	}

	@Test
	void 한_값이_다른_값에_포함되면_긴_값만_남긴다() {
		Detector shortOne = (text, exclude) -> List.of(new DetectedEntity("강남구", MaskingType.ADDRESS));
		Detector longOne = (text, exclude) -> List.of(new DetectedEntity("서울시 강남구 테헤란로 123", MaskingType.ADDRESS));
		CompositeDetector composite = new CompositeDetector(List.of(shortOne, longOne));

		List<DetectedEntity> result = composite.detect("서울시 강남구 테헤란로 123", Set.of());

		assertThat(result).containsExactly(new DetectedEntity("서울시 강남구 테헤란로 123", MaskingType.ADDRESS));
	}

	@Test
	void 같은_값이_두_탐지기에서_나오면_먼저_온_쪽의_유형을_유지한다() {
		Detector first = (text, exclude) -> List.of(new DetectedEntity("500만원", MaskingType.AMOUNT));
		Detector second = (text, exclude) -> List.of(new DetectedEntity("500만원", MaskingType.ETC));
		CompositeDetector composite = new CompositeDetector(List.of(first, second));

		assertThat(composite.detect("500만원", Set.of()))
				.containsExactly(new DetectedEntity("500만원", MaskingType.AMOUNT));
	}

	@Test
	void 자기_자신이_목록에_들어와도_무시한다() {
		CompositeDetector inner = new CompositeDetector(List.of(new FixedValueDetector()));
		CompositeDetector composite = new CompositeDetector(List.of(inner, new RegexDetector()));

		assertThat(composite.detect("홍길동 a@b.com", Set.of()))
				.containsExactly(new DetectedEntity("a@b.com", MaskingType.EMAIL));
	}

	@Test
	void excludeTypes를_모든_탐지기에_전달한다() {
		CompositeDetector composite = new CompositeDetector(List.of(new FixedValueDetector(), new RegexDetector()));

		List<DetectedEntity> result = composite.detect("홍길동 010-1234-5678", Set.of(MaskingType.NAME, MaskingType.PHONE));

		assertThat(result).isEmpty();
	}
}
