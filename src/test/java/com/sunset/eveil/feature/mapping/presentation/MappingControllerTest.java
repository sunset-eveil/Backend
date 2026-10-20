package com.sunset.eveil.feature.mapping.presentation;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sunset.eveil.feature.mapping.application.GetMappingService;
import com.sunset.eveil.feature.mapping.domain.EncryptedMapping;
import com.sunset.eveil.feature.mapping.domain.MappingRepository;
import com.sunset.eveil.global.auth.*;
import com.sunset.eveil.global.crypto.Sha256Hasher;
import com.sunset.eveil.global.error.GlobalExceptionHandler;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MappingControllerTest {

	private static final String CLIENT_ID = "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d";
	private static final String CHAT_ID = "ffffffff-ffff-4fff-8fff-ffffffffffff";
	private static final byte[] AUTH_KEY = new byte[] {1, 2, 3};
	private final MappingRepository repository = mock(MappingRepository.class);
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		Sha256Hasher hasher = new Sha256Hasher();
		ClientCredentialsPort credentials = clientId -> CLIENT_ID.equals(clientId)
				? Optional.of(new ClientCredentials(CLIENT_ID, hasher.hash(AUTH_KEY), new byte[40]))
				: Optional.empty();
		mvc = MockMvcBuilders.standaloneSetup(new MappingController(new GetMappingService(repository)))
				.setCustomArgumentResolvers(new ClientAuthArgumentResolver())
				.addInterceptors(new ClientAuthInterceptor(credentials, hasher))
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void KEK_없이_암호문을_그대로_조회하고_캐시를_금지한다() throws Exception {
		when(repository.findByClientIdAndChatId(CLIENT_ID, CHAT_ID))
				.thenReturn(Optional.of(new EncryptedMapping("aXY=", "Y2lwaGVydGV4dA==")));
		mvc.perform(authenticated(CHAT_ID)).andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.wrappedAesKey").value(Base64.getEncoder().encodeToString(new byte[40])))
				.andExpect(jsonPath("$.iv").value("aXY="))
				.andExpect(jsonPath("$.ciphertext").value("Y2lwaGVydGV4dA=="));
		verify(repository).findByClientIdAndChatId(CLIENT_ID, CHAT_ID);
	}

	@Test
	void 매핑이_없으면_404이다() throws Exception {
		when(repository.findByClientIdAndChatId(CLIENT_ID, CHAT_ID)).thenReturn(Optional.empty());
		mvc.perform(authenticated(CHAT_ID)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("해당 대화의 매핑 정보가 없습니다."));
	}

	@Test
	void 인증_실패시_매핑을_조회하지_않는다() throws Exception {
		mvc.perform(get("/api/chats/{chatId}/mappings", CHAT_ID)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/chats/{chatId}/mappings", CHAT_ID)
				.header(AuthHeaders.CLIENT_ID, CLIENT_ID).header(AuthHeaders.AUTH_KEY, "BA=="))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(repository);
	}

	@Test
	void 잘못된_chatId와_헤더는_400이다() throws Exception {
		mvc.perform(authenticated("invalid")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/chats/{chatId}/mappings", CHAT_ID)
				.header(AuthHeaders.CLIENT_ID, "invalid").header(AuthHeaders.AUTH_KEY, "AQ=="))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(repository);
	}

	@Test
	void 미등록_클라이언트는_401이고_매핑을_조회하지_않는다() throws Exception {
		mvc.perform(get("/api/chats/{chatId}/mappings", CHAT_ID)
				.header(AuthHeaders.CLIENT_ID, "11111111-1111-4111-8111-111111111111")
				.header(AuthHeaders.AUTH_KEY, Base64.getEncoder().encodeToString(AUTH_KEY)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("클라이언트 인증에 실패했습니다."));
		verifyNoInteractions(repository);
	}

	@Test
	void authKey가_Base64가_아니면_400이다() throws Exception {
		mvc.perform(get("/api/chats/{chatId}/mappings", CHAT_ID)
				.header(AuthHeaders.CLIENT_ID, CLIENT_ID).header(AuthHeaders.AUTH_KEY, "invalid!!"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
		verifyNoInteractions(repository);
	}

	@Test
	void 조회_오류는_내부정보_없이_500이다() throws Exception {
		when(repository.findByClientIdAndChatId(CLIENT_ID, CHAT_ID))
				.thenThrow(new IllegalStateException("sensitive storage details"));
		mvc.perform(authenticated(CHAT_ID)).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
	}

	private MockHttpServletRequestBuilder authenticated(String chatId) {
		return get("/api/chats/{chatId}/mappings", chatId).header(AuthHeaders.CLIENT_ID, CLIENT_ID)
				.header(AuthHeaders.AUTH_KEY, Base64.getEncoder().encodeToString(AUTH_KEY));
	}
}
