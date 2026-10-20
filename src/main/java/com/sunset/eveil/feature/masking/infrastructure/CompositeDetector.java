package com.sunset.eveil.feature.masking.infrastructure;

import com.sunset.eveil.feature.masking.domain.DetectedEntity;
import com.sunset.eveil.feature.masking.domain.Detector;
import com.sunset.eveil.global.mask.MaskingType;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 여러 탐지기의 결과를 합친다. MaskTextService는 @Primary인 이 빈을 주입받는다.
 * - 같은 값이 여러 탐지기에서 나오면 목록에서 먼저 온 탐지기의 유형을 유지한다.
 * - 한 값이 다른 값에 포함되면(겹침) 긴 값만 남긴다.
 * - 결과는 긴 값부터 정렬한다.
 * Spring은 List<Detector>를 주입할 때 자기 자신을 빼지만, 혹시 들어오더라도 생성자에서 한 번 더 걸러낸다.
 */
@Component
@Primary
public class CompositeDetector implements Detector {

	private final List<Detector> detectors;

	public CompositeDetector(List<Detector> detectors) {
		this.detectors = detectors.stream()
				.filter(detector -> !(detector instanceof CompositeDetector))
				.toList();
	}

	@Override
	public List<DetectedEntity> detect(String text, Set<MaskingType> excludeTypes) {
		Map<String, DetectedEntity> byValue = new LinkedHashMap<>();
		for (Detector detector : detectors) {
			for (DetectedEntity entity : detector.detect(text, excludeTypes)) {
				byValue.putIfAbsent(entity.value(), entity);
			}
		}
		List<DetectedEntity> merged = List.copyOf(byValue.values());
		return merged.stream()
				.filter(entity -> !isContainedInLonger(entity, merged))
				.sorted(Comparator.comparingInt((DetectedEntity entity) -> entity.value().length()).reversed())
				.toList();
	}

	private static boolean isContainedInLonger(DetectedEntity entity, List<DetectedEntity> all) {
		return all.stream().anyMatch(other ->
				other.value().length() > entity.value().length() && other.value().contains(entity.value()));
	}
}
