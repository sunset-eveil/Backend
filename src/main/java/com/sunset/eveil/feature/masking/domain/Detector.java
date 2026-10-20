package com.sunset.eveil.feature.masking.domain;

import com.sunset.eveil.global.mask.MaskingType;
import java.util.List;
import java.util.Set;

/**
 * 개인정보 탐지기. 결과는 중복 제거·trim 후 긴 값부터 돌려준다.
 * excludeTypes가 비어 있으면 전체 유형을 탐지한다.
 */
public interface Detector {

	List<DetectedEntity> detect(String text, Set<MaskingType> excludeTypes);
}
