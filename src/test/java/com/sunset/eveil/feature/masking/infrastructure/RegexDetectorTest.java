package com.sunset.eveil.feature.masking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunset.eveil.global.mask.MaskingType;
import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegexDetectorTest {

	private final RegexDetector detector = new RegexDetector();

	@Test
	void 전화번호를_탐지한다() {
		List<DetectedEntity> result = detector.detect("연락처는 010-1234-5678 또는 02-123-4567, 01012345678 입니다.", Set.of());

		assertThat(result).containsExactlyInAnyOrder(
				new DetectedEntity("010-1234-5678", MaskingType.PHONE),
				new DetectedEntity("02-123-4567", MaskingType.PHONE),
				new DetectedEntity("01012345678", MaskingType.PHONE));
	}

	@Test
	void 이메일을_탐지한다() {
		List<DetectedEntity> result = detector.detect("메일은 kwon.su-hyun+dev@example.co.kr 로 주세요", Set.of());

		assertThat(result).containsExactly(new DetectedEntity("kwon.su-hyun+dev@example.co.kr", MaskingType.EMAIL));
	}

	@Test
	void 주민등록번호는_검증번호가_맞을_때만_탐지한다() {
		assertThat(detector.detect("주민번호 900101-1234568 입니다", Set.of()))
				.containsExactly(new DetectedEntity("900101-1234568", MaskingType.RRN));
		assertThat(detector.detect("주민번호 900101-1234567 입니다", Set.of()))
				.noneMatch(entity -> entity.type() == MaskingType.RRN);
	}

	@Test
	void 카드번호는_Luhn_검증을_통과할_때만_탐지한다() {
		assertThat(detector.detect("카드 4111-1111-1111-1111 결제", Set.of()))
				.containsExactly(new DetectedEntity("4111-1111-1111-1111", MaskingType.CARD_ACCOUNT));
		assertThat(detector.detect("카드 4111-1111-1111-1112 결제", Set.of()))
				.noneMatch(entity -> entity.type() == MaskingType.CARD_ACCOUNT);
	}

	@Test
	void 카드번호_안의_숫자를_전화번호로_중복_탐지하지_않는다() {
		List<DetectedEntity> result = detector.detect("4010-1234-5678-9011", Set.of());

		assertThat(result).noneMatch(entity -> entity.type() == MaskingType.PHONE);
	}

	@Test
	void 이메일_안의_숫자를_전화번호로_탐지하지_않는다() {
		List<DetectedEntity> result = detector.detect("01012345678@example.com", Set.of());

		assertThat(result).containsExactly(new DetectedEntity("01012345678@example.com", MaskingType.EMAIL));
	}

	@Test
	void 같은_값이_여러_번_나와도_한_번만_돌려준다() {
		List<DetectedEntity> result = detector.detect("010-1234-5678 그리고 010-1234-5678", Set.of());

		assertThat(result).hasSize(1);
	}

	@Test
	void excludeTypes에_있는_유형은_탐지하지_않는다() {
		List<DetectedEntity> result = detector.detect("a@b.com 010-1234-5678", Set.of(MaskingType.PHONE));

		assertThat(result).containsExactly(new DetectedEntity("a@b.com", MaskingType.EMAIL));
	}

	@Test
	void 긴_값부터_돌려준다() {
		List<DetectedEntity> result = detector.detect("02-123-4567 / 4111-1111-1111-1111 / a@b.com", Set.of());

		assertThat(result).extracting(DetectedEntity::value)
				.containsExactly("4111-1111-1111-1111", "02-123-4567", "a@b.com");
	}
}
