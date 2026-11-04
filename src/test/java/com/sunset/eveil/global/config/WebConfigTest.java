package com.sunset.eveil.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "app.cors.allowed-origins=https://localhost:5173,chrome-extension://test-extension")
class WebConfigTest {

	private static final String PATH = "/api/chats/11111111-2222-4333-8444-555555555555/mappings";

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Test
	void 허용한_프론트의_사전요청은_인증키_없이_통과한다() throws Exception {
		mvc.perform(options(PATH)
				.header("Origin", "https://localhost:5173")
				.header("Access-Control-Request-Method", "PUT")
				.header("Access-Control-Request-Headers", "content-type,x-client-id,x-auth-key,x-kek"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "https://localhost:5173"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
	}

	@Test
	void 허용하지_않은_주소의_사전요청은_거부한다() throws Exception {
		mvc.perform(options(PATH)
				.header("Origin", "https://untrusted.example")
				.header("Access-Control-Request-Method", "PUT"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void 확장프로그램의_실제요청은_여전히_인증이_필요하다() throws Exception {
		mvc.perform(get(PATH).header("Origin", "chrome-extension://test-extension"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Access-Control-Allow-Origin", "chrome-extension://test-extension"));
	}
}
