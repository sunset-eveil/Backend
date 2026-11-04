package com.sunset.eveil.global.config;

import com.sunset.eveil.global.auth.ClientAuthArgumentResolver;
import com.sunset.eveil.global.auth.ClientAuthInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final ClientAuthInterceptor clientAuthInterceptor;
	private final ClientAuthArgumentResolver clientAuthArgumentResolver;

	@Value("${app.cors.allowed-origins:}")
	private List<String> allowedOrigins;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		String[] origins = allowedOrigins.stream()
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toArray(String[]::new);
		if (origins.length > 0) {
			registry.addMapping("/api/**")
					.allowedOrigins(origins)
					.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
					.allowedHeaders("Content-Type", "X-Client-Id", "X-Auth-Key", "X-Kek")
					.allowCredentials(false)
					.maxAge(3600);
		}
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(clientAuthInterceptor)
				.addPathPatterns("/api/**")
				// 클라이언트 등록(POST /api/clients)은 인증 대상이 아니다. 이 경로에는 POST만 존재한다.
				.excludePathPatterns("/api/clients");
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(clientAuthArgumentResolver);
	}
}
