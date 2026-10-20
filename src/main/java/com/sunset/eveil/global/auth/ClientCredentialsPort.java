package com.sunset.eveil.global.auth;

import java.util.Optional;

/**
 * 인터셉터가 클라이언트 인증 정보를 조회하는 창구.
 * global → feature 직접 의존을 막기 위해 인터페이스만 두고, 구현은 feature/client/infrastructure가 맡는다.
 */
public interface ClientCredentialsPort {

	Optional<ClientCredentials> findCredentials(String clientId);
}
