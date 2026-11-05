package com.sunset.eveil.feature.masking.infrastructure;

import java.util.List;

/** AI 호출 어댑터. 원문과 결과를 저장하거나 로깅하지 않는다. */
@FunctionalInterface
public interface AiInferenceClient {

	List<AiDetectedSpan> detect(String text);
}
