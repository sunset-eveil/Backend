package com.sunset.eveil.feature.masking.infrastructure;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 요청별 원문과 응답은 지역 변수로만 유지하며 외부 오류 본문을 예외에 포함하지 않는다. */
@Component
@ConditionalOnProperty(name = "app.masking.detector", havingValue = "ai", matchIfMissing = true)
public class HttpAiInferenceClient implements AiInferenceClient {

	private final RestClient client;

	public HttpAiInferenceClient(@Value("${app.ai.base-url:http://127.0.0.1:8000}") String baseUrl,
			@Value("${app.ai.timeout:30s}") Duration timeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(timeout);
		factory.setReadTimeout(timeout);
		this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
	}

	@Override
	public List<AiDetectedSpan> detect(String text) {
		String requestId = UUID.randomUUID().toString();
		byte[] body = tools.jackson.databind.json.JsonMapper.builder().build()
				.writeValueAsBytes(Map.of("request_id", requestId, "text", text));
		AiResponse response = client.post().uri("/api/v1/pii/detect")
				.contentType(MediaType.APPLICATION_JSON)
				.contentLength(body.length)
				.body(body)
				.retrieve()
				.onStatus(status -> !status.is2xxSuccessful(), (request, result) -> {
					throw new IllegalStateException("AI detection request failed");
				})
				.body(AiResponse.class);
		if (response == null || !requestId.equals(response.request_id())
				|| !"unicode_codepoint".equals(response.offset_unit()) || response.entities() == null) {
			throw new IllegalStateException("Invalid AI detection response");
		}
		return response.entities();
	}

	public record AiResponse(String request_id, String model_version, String offset_unit,
			List<AiDetectedSpan> entities) {
	}
}
