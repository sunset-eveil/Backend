package com.sunset.eveil.feature.masking.infrastructure;

/** 0 기반 유니코드 코드 포인트 범위 [start, end). */
public record AiDetectedSpan(int start, int end, String type) {

	public AiDetectedSpan {
		if (start < 0 || end <= start || type == null) {
			throw new IllegalArgumentException("Invalid AI detection span");
		}
	}
}
