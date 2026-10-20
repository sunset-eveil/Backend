package com.sunset.eveil.feature.client.domain;

import java.util.Optional;

public interface ClientRepository {

	boolean exists(String clientId);

	void save(Client client);

	Optional<Client> findById(String clientId);
}
