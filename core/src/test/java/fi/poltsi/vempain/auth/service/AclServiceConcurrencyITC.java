package fi.poltsi.vempain.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(classes = fi.poltsi.vempain.auth.TestApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AclServiceConcurrencyITC {

	@Container
	public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	static {
		// Ensure container is started early so DynamicPropertySource can query mapped ports
		postgres.start();
	}

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@Autowired
	private AclService aclService;


	@Test
	void allocatesUniqueIdsUnderConcurrency() throws Exception {
		var threads = Integer.parseInt(System.getenv()
											 .getOrDefault("ACL_TEST_THREADS", "50"));
		var ops     = Integer.parseInt(System.getenv()
											 .getOrDefault("ACL_TEST_OPS", "1000"));

		var ex     = Executors.newFixedThreadPool(threads);
		var seen   = new ConcurrentHashMap<Long, Boolean>();
		var errors = new CopyOnWriteArrayList<Throwable>();
		var latch  = new CountDownLatch(ops);

		for (int i = 0; i < ops; i++) {
			ex.submit(() -> {
				try {
					var acl = aclService.createUniqueAcl(1L, null, true, true, true, true);

					if (acl == null) {
						errors.add(new IllegalStateException("Returned null"));
					} else {
						long id = acl.getAclId();
						if (seen.putIfAbsent(id, Boolean.TRUE) != null) {
							errors.add(new IllegalStateException("Duplicate id: " + id));
						}
					}
				} catch (Throwable t) {
					errors.add(t);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(60, TimeUnit.SECONDS);
		ex.shutdownNow();

		if (!errors.isEmpty()) {
			for (Throwable t : errors) {
				t.printStackTrace();
			}
		}

		assertTrue(errors.isEmpty(), "There were errors during concurrent creation");
		assertEquals(ops, seen.size(), "Not all ACLs were created/unique");
	}
}
