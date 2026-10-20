package com.sunset.eveil.feature.masking.domain;

import java.util.Comparator;
import java.util.List;

/**
 * 마스킹 요청 원문. 탐지 결과를 원문 기준으로 검증한다 (CLAUDE.md 6번 "원문 검증").
 */
public record OriginalText(String text) {

	public OriginalText {
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("text must not be blank");
		}
	}

	/** 원문에 실제로 존재하는 값만 남기고, 긴 값부터 정렬한다. */
	public List<DetectedEntity> keepPresent(List<DetectedEntity> detected) {
		return detected.stream()
				.filter(entity -> text.contains(entity.value()))
				.sorted(Comparator.comparingInt((DetectedEntity entity) -> entity.value().length()).reversed())
				.toList();
	}
}
