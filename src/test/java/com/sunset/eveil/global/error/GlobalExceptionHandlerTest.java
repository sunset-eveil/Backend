package com.sunset.eveil.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.MissingRequestHeaderException;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void BusinessException은_자신의_상태코드와_메시지로_응답한다() {
		ResponseEntity<ErrorResponse> response =
				handler.handleBusiness(new BusinessException(HttpStatus.CONFLICT, "이미 등록된 클라이언트입니다."));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody()).isEqualTo(new ErrorResponse(409, "이미 등록된 클라이언트입니다."));
	}

	@Test
	void 본문_파싱_실패는_400이다() {
		HttpMessageNotReadableException e =
				new HttpMessageNotReadableException("broken json", new MockHttpInputMessage(new byte[0]));

		ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().message()).isEqualTo(GlobalExceptionHandler.INVALID_REQUEST_MESSAGE);
	}

	@Test
	void 헤더_누락은_401이다() throws NoSuchMethodException {
		MethodParameter parameter =
				new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class), 0);

		ResponseEntity<ErrorResponse> response =
				handler.handleMissingHeader(new MissingRequestHeaderException("X-Client-Id", parameter));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getBody().message()).isEqualTo(GlobalExceptionHandler.AUTH_FAILED_MESSAGE);
	}

	@Test
	void 예상_못한_예외는_500이고_원문_메시지를_노출하지_않는다() {
		ResponseEntity<ErrorResponse> response =
				handler.handleUnexpected(new IllegalStateException("권수현 010-1111-1111"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().message())
				.isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR_MESSAGE)
				.doesNotContain("권수현");
	}

	@SuppressWarnings("unused")
	void sample(String header) {
	}
}
