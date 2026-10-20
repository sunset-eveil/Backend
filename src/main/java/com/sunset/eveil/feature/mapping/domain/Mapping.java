package com.sunset.eveil.feature.mapping.domain;

import com.sunset.eveil.global.mask.MaskingType;
/**
 * 마스킹 코드 ↔ 원문 값 한 쌍. 유형은 maskCode에 포함되어 있다.
 */
public record Mapping(MaskCode maskCode, String value) {

	public Mapping {
		if (maskCode == null) {
			throw new IllegalArgumentException("maskCode must not be null");
		}
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
	}

	public MaskingType type() {
		return maskCode.type();
	}
}
