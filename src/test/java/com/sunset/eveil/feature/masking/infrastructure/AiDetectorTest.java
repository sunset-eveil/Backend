package com.sunset.eveil.feature.masking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiDetectorTest {

	@Test
	void 이모지와_줄바꿈이_있는_원문을_변경하지_않고_코드포인트로_추출한다() {
		String text = " 😀김민수\n김민수";
		AiDetector detector = new AiDetector(original -> {
			assertThat(original).isEqualTo(text);
			return List.of(new AiDetectedSpan(2, 5, "NAME"), new AiDetectedSpan(6, 9, "NAME"));
		});
		assertThat(detector.detect(text, null))
				.containsExactly(new DetectedEntity("김민수", MaskingType.NAME));
	}

	@Test
	void 카드와_계좌를_통합하고_제외유형을_적용한다() {
		AiDetector detector = new AiDetector(text -> List.of(
				new AiDetectedSpan(0, 3, "CARD"), new AiDetectedSpan(4, 8, "ACCOUNT")));
		assertThat(detector.detect("123 4567", Set.of())).containsExactly(
				new DetectedEntity("4567", MaskingType.CARD_ACCOUNT),
				new DetectedEntity("123", MaskingType.CARD_ACCOUNT));
		assertThat(detector.detect("123 4567", Set.of(MaskingType.CARD_ACCOUNT))).isEmpty();
	}

	@Test
	void 금액을_추출하고_제외유형을_적용한다() {
		AiDetector detector = new AiDetector(text -> List.of(new AiDetectedSpan(6, 13, "AMOUNT")));
		assertThat(detector.detect("결제 금액 50,000원", Set.of()))
				.containsExactly(new DetectedEntity("50,000원", MaskingType.AMOUNT));
		assertThat(detector.detect("결제 금액 50,000원", Set.of(MaskingType.AMOUNT))).isEmpty();
	}

	@Test
	void 잘못된_범위와_유형은_실패한다() {
		AiDetector invalidRange = new AiDetector(text -> List.of(new AiDetectedSpan(0, 4, "NAME")));
		AiDetector invalidType = new AiDetector(text -> List.of(new AiDetectedSpan(0, 3, "OTHER")));
		assertThatThrownBy(() -> invalidRange.detect("김민수", Set.of()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> invalidType.detect("김민수", Set.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 탐지없음과_호출실패를_구분한다() {
		assertThat(new AiDetector(text -> List.of()).detect("문장", Set.of())).isEmpty();
		AiDetector detector = new AiDetector(text -> {
			throw new IllegalStateException("AI unavailable");
		});
		assertThatThrownBy(() -> detector.detect("문장", Set.of()))
				.isInstanceOf(IllegalStateException.class);
	}
}
