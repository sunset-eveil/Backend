package com.sunset.eveil.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 로깅. method · path · status · 소요시간만 남긴다.
 * X-Auth-Key, X-Kek 헤더와 요청 본문, query string은 읽지도 기록하지도 않는다.
 */
@Component
public class SensitiveHeaderFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(SensitiveHeaderFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long startedAt = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		} finally {
			long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
			log.info("{} {} -> {} ({} ms)",
					request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMillis);
		}
	}
}
