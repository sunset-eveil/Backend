package com.sunset.eveil.global.auth;

/**
 * 인증 관련 헤더 이름. docs/API.md 공통 사항 기준.
 */
public final class AuthHeaders {

	public static final String CLIENT_ID = "X-Client-Id";
	public static final String AUTH_KEY = "X-Auth-Key";
	public static final String KEK = "X-Kek";

	private AuthHeaders() {
	}
}
