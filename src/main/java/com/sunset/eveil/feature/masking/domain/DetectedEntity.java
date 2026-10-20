package com.sunset.eveil.feature.masking.domain;

import com.sunset.eveil.global.mask.MaskingType;

/**
 * 탐지기가 찾은 (값, 유형). 번호는 없다. 번호는 MappingTable이 매긴다.
 */
public record DetectedEntity(String value, MaskingType type) {

	public DetectedEntity {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
		if (type == null) {
			throw new IllegalArgumentException("type must not be null");
		}
		value = value.trim();
	}
}
