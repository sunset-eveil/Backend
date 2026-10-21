package com.sunset.eveil.feature.mapping.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.*;
import com.sunset.eveil.feature.mapping.application.UnmaskMappingService;
import com.sunset.eveil.feature.mapping.application.UpdateMappingTypeService;
import com.sunset.eveil.feature.mapping.domain.MappingTable;
import com.sunset.eveil.global.crypto.AesKeyGenerator;
import com.sunset.eveil.global.crypto.KeyWrapper;
import com.sunset.eveil.global.lock.ChatLockManager;
import com.sunset.eveil.global.mask.MaskingType;
import javax.crypto.SecretKey;
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
	private final byte[] kek = new byte[32];
	private final SecretKey aesKey = new AesKeyGenerator().generate();
	private final byte[] wrappedAesKey = new KeyWrapper().wrap(kek, aesKey);

	@BeforeEach
	void setUp() {
		Sha256Hasher hasher = new Sha256Hasher();
		ClientCredentialsPort credentials = clientId -> CLIENT_ID.equals(clientId)
				? Optional.of(new ClientCredentials(CLIENT_ID, hasher.hash(AUTH_KEY), wrappedAesKey))
				: Optional.empty();
		mvc = MockMvcBuilders.standaloneSetup(new MappingController(new GetMappingService(repository),
				new UnmaskMappingService(repository,
						new KeyWrapper(), new ChatLockManager()),
				new UpdateMappingTypeService(
						repository, new KeyWrapper(), new ChatLockManager())))
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
				.andExpect(jsonPath("$.wrappedAesKey").value(Base64.getEncoder().encodeToString(wrappedAesKey)))
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

	@Test
	void 삭제는_204이고_카운터를_유지한_매핑을_저장한다() throws Exception {
		var table = MappingTable.empty();
		table.assign("홍길동", MaskingType.NAME);
		when(repository.find(eq(CLIENT_ID), eq(CHAT_ID), any())).thenReturn(Optional.of(table));
		mvc.perform(deleteRequest("NAME1")).andExpect(status().isNoContent()).andExpect(content().string(""));
		assertThat(table.isEmpty()).isTrue();
		assertThat(table.assign("홍길동", MaskingType.NAME).text())
				.isEqualTo("NAME2");
		verify(repository).save(eq(CLIENT_ID), eq(CHAT_ID), same(table), any());
	}

	@Test
	void 대화나_코드가_없으면_404이고_저장하지_않는다() throws Exception {
		when(repository.find(eq(CLIENT_ID), eq(CHAT_ID), any())).thenReturn(Optional.empty());
		mvc.perform(deleteRequest("NAME1")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("해당 마스킹 정보를 찾을 수 없습니다."));
		when(repository.find(eq(CLIENT_ID), eq(CHAT_ID), any()))
				.thenReturn(Optional.of(MappingTable.empty()));
		mvc.perform(deleteRequest("NAME1")).andExpect(status().isNotFound());
		verify(repository, never()).save(any(), any(), any(), any());
	}

	@Test
	void 잘못된_코드는_400이고_저장소를_호출하지_않는다() throws Exception {
		for (String code : new String[] {"name1", "NAME0", "UNKNOWN1", "NAME99999999999999999", "NAME01"}) {
			mvc.perform(deleteRequest(code)).andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value("마스킹 코드 형식이 올바르지 않습니다."));
		}
		verifyNoInteractions(repository);
	}

	@Test
	void 삭제_인증과_KEK를_검증한다() throws Exception {
		mvc.perform(delete(
				"/api/chats/{chatId}/mappings/NAME1", CHAT_ID)).andExpect(status().isUnauthorized());
		mvc.perform(deleteRequest("NAME1", "invalid!!")).andExpect(status().isBadRequest());
		byte[] wrongKek = new byte[32];
		wrongKek[0] = 1;
		var request = deleteRequest("NAME1", Base64.getEncoder().encodeToString(wrongKek));
		mvc.perform(request).andExpect(status().isForbidden());
		verifyNoInteractions(repository);
	}

	private MockHttpServletRequestBuilder deleteRequest(String code) {
		return deleteRequest(code, Base64.getEncoder().encodeToString(kek));
	}

	private MockHttpServletRequestBuilder deleteRequest(String code, String kekBase64) {
		return delete(
				"/api/chats/{chatId}/mappings/{maskCode}", CHAT_ID, code)
				.header(AuthHeaders.CLIENT_ID, CLIENT_ID)
				.header(AuthHeaders.AUTH_KEY, Base64.getEncoder().encodeToString(AUTH_KEY))
				.header(AuthHeaders.KEK, kekBase64);
	}

	private MockHttpServletRequestBuilder authenticated(String chatId) {
		return get("/api/chats/{chatId}/mappings", chatId).header(AuthHeaders.CLIENT_ID, CLIENT_ID)
				.header(AuthHeaders.AUTH_KEY, Base64.getEncoder().encodeToString(AUTH_KEY));
	}

	@Test
	void PUT은_새_매핑을_생성하고_값을_trim한다() throws Exception {
		when(repository.find(eq(CLIENT_ID), eq(CHAT_ID), any())).thenReturn(Optional.empty());
		mvc.perform(putRequest("{\"value\":\"  홍길동  \",\"type\":\"NAME\"}"))
				.andExpect(status().isNoContent()).andExpect(content().string(""));
		var captor = org.mockito.ArgumentCaptor.forClass(MappingTable.class);
		verify(repository).save(eq(CLIENT_ID), eq(CHAT_ID), captor.capture(), any());
		assertThat(captor.getValue().findByValue("홍길동"))
				.map(mapping -> mapping.maskCode().text()).contains("NAME1");
	}

	@Test
	void PUT은_기존_유형을_변경한다() throws Exception {
		var table = MappingTable.empty();
		table.assign("홍길동", MaskingType.NAME);
		when(repository.find(eq(CLIENT_ID), eq(CHAT_ID), any())).thenReturn(Optional.of(table));
		mvc.perform(putRequest("{\"value\":\"홍길동\",\"type\":\"ADDRESS\"}"))
				.andExpect(status().isNoContent());
		assertThat(table.findByValue("홍길동")).map(mapping -> mapping.maskCode().text()).contains("ADDRESS1");
		verify(repository).save(eq(CLIENT_ID), eq(CHAT_ID), same(table), any());
	}

	@Test
	void PUT은_빈값과_미정의_유형을_거부한다() throws Exception {
		mvc.perform(putRequest("{\"value\":\"   \",\"type\":\"NAME\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("지정할 값이 비어 있습니다."));
		for (String body : new String[] {"{}", "{\"value\":\"홍길동\"}",
				"{\"value\":\"홍길동\",\"type\":\"UNKNOWN\"}"}) {
			mvc.perform(putRequest(body)).andExpect(status().isBadRequest());
		}
		verifyNoInteractions(repository);
	}

	@Test
	void PUT은_인증과_KEK를_검증한다() throws Exception {
		String body = "{\"value\":\"홍길동\",\"type\":\"NAME\"}";
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/chats/{chatId}/mappings", CHAT_ID)
				.contentType("application/json").content(body)).andExpect(status().isUnauthorized());
		mvc.perform(putRequest(body, "invalid!!"))
				.andExpect(status().isBadRequest());
		byte[] wrongKek = new byte[32];
		wrongKek[0] = 1;
		mvc.perform(putRequest(body, Base64.getEncoder().encodeToString(wrongKek)))
				.andExpect(status().isForbidden());
		verifyNoInteractions(repository);
	}

	private MockHttpServletRequestBuilder putRequest(String body) {
		return putRequest(body, Base64.getEncoder().encodeToString(kek));
	}

	private MockHttpServletRequestBuilder putRequest(String body, String kekBase64) {
		return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/chats/{chatId}/mappings", CHAT_ID)
				.header(AuthHeaders.CLIENT_ID, CLIENT_ID)
				.header(AuthHeaders.AUTH_KEY, Base64.getEncoder().encodeToString(AUTH_KEY))
				.header(AuthHeaders.KEK, kekBase64).contentType("application/json").content(body);
	}
}
