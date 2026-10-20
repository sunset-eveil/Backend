package com.sunset.eveil.global.lock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ChatLockManagerTest {

	private final ChatLockManager lockManager = new ChatLockManager();

	@Test
	void 같은_chatId의_동시_요청은_직렬화된다() throws InterruptedException {
		int[] counter = {0};
		int threads = 8;
		int iterations = 1_000;
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		for (int t = 0; t < threads; t++) {
			executor.submit(() -> {
				for (int i = 0; i < iterations; i++) {
					lockManager.withLock("client", "chat", () -> counter[0]++);
				}
			});
		}
		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

		assertThat(counter[0]).isEqualTo(threads * iterations);
	}

	@Test
	void 작업의_반환값을_그대로_돌려준다() {
		assertThat(lockManager.withLock("client", "chat", () -> "result")).isEqualTo("result");
	}
}
