package com.sunset.eveil.global.lock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * chatId 단위 동시 요청 제어. 단일 서버 전제의 ReentrantLock 맵.
 * 다중 서버로 확장되면 같은 시그니처를 유지한 채 Redis 락(SET NX PX)으로 교체한다.
 */
@Component
public class ChatLockManager {

	private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

	public <T> T withLock(String clientId, String chatId, Supplier<T> action) {
		ReentrantLock lock = locks.computeIfAbsent(clientId + ":" + chatId, key -> new ReentrantLock());
		lock.lock();
		try {
			return action.get();
		} finally {
			lock.unlock();
		}
	}
}
