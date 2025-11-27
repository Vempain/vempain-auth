package fi.poltsi.vempain.auth.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = fi.poltsi.vempain.auth.TestApplicationConfig.class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AclIdGeneratorConcurrencyITC {

	@Container
	public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	static {
		// Ensure container is started before DynamicPropertySource tries to read mapped ports
		if (!postgres.isRunning()) {
			postgres.start();
		}
	}

	@Autowired
	private AclIdGenerator aclIdGenerator;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		// enable flyway
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@AfterAll
	void afterAll() {
		// cleanup if needed
		if (postgres != null && postgres.isRunning()) {
			postgres.stop();
		}
	}

	@Test
	void allocatesUniqueIdsUnderConcurrency() throws Exception {
		final int threads   = 100;
		final int perThread = 200; // total 20k
		final int total     = threads * perThread;

		var ex         = Executors.newFixedThreadPool(threads);
		var startLatch = new CountDownLatch(1);
		var doneLatch  = new CountDownLatch(total);

		var ids      = Collections.newSetFromMap(new ConcurrentHashMap<>());
		var exHolder = new Exception[1];

		for (int t = 0; t < threads; t++) {
			ex.submit(() -> {
				try {
					startLatch.await();
					for (int i = 0; i < perThread; i++) {
						long id = aclIdGenerator.nextAclId();
						if (!ids.add(id)) {
							exHolder[0] = new IllegalStateException("Duplicate id: " + id);
						}
						doneLatch.countDown();
					}
				} catch (Exception e) {
					exHolder[0] = e;
				}
			});
		}

		startLatch.countDown();
		var finished = doneLatch.await(60, TimeUnit.SECONDS);
		ex.shutdownNow();

		if (exHolder[0] != null) {
			throw exHolder[0];
		}
		assertTrue(finished, "Tasks did not finish in time");
		assertEquals(total, ids.size());
	}
}
