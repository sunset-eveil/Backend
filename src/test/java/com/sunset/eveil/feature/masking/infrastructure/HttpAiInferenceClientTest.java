package com.sunset.eveil.feature.masking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class HttpAiInferenceClientTest {

	@Test
	void 실제_HTTP로_요청하고_AI_응답을_읽는다() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/pii/detect", exchange -> {
			if (exchange.getRequestHeaders().getFirst("Content-Length") == null) {
				exchange.sendResponseHeaders(400, -1);
				exchange.close();
				return;
			}
			String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			var matcher = Pattern.compile("\"request_id\"\\s*:\\s*\"([^\"]+)\"").matcher(request);
			if (!matcher.find()) {
				throw new IllegalStateException("Missing request ID");
			}
			byte[] body = ("{\"request_id\":\"" + matcher.group(1)
					+ "\",\"model_version\":\"eveil-ai-v1\",\"offset_unit\":\"unicode_codepoint\","
					+ "\"entities\":[{\"start\":1,\"end\":4,\"type\":\"NAME\"}]}")
					.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		try {
			var client = new HttpAiInferenceClient("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));
			assertThat(client.detect("😀김민수")).containsExactly(new AiDetectedSpan(1, 4, "NAME"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void AI_오류는_빈목록으로_바꾸거나_응답본문을_노출하지_않는다() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/pii/detect", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});
		server.start();
		try {
			var client = new HttpAiInferenceClient("http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(2));
			assertThatThrownBy(() -> client.detect("김민수"))
					.isInstanceOf(IllegalStateException.class).hasMessage("AI detection request failed");
		} finally {
			server.stop(0);
		}
	}
}
