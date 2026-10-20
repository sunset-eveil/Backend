package com.sunset.eveil.feature.masking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.feature.masking.domain.Detector;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Spring이 Detector 주입 지점에 @Primary CompositeDetector를 고르고, 고정값·정규식 둘 다 포함하는지 확인. */
@SpringBootTest
class DetectorWiringTest {

	@Autowired
	private Detector detector;

	@Test
	void 주입되는_Detector는_고정값과_정규식을_합친_Composite다() {
		assertThat(detector).isInstanceOf(CompositeDetector.class);
		assertThat(detector.detect("안녕하세요 홍길동입니다. 연락처는 010-1234-5678", Set.of()))
				.containsExactly(
						new DetectedEntity("010-1234-5678", MaskingType.PHONE),
						new DetectedEntity("홍길동", MaskingType.NAME));
	}
}
