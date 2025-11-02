package fi.poltsi.vempain.auth;

import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.tools.TestITCTools;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("test")
public abstract class IntegrationTestSetup {

	private static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("vempain_auth_test")
					.withUsername("test")
					.withPassword("test");

	@Autowired
	protected TestITCTools testITCTools;
	@Autowired
	protected AclService   aclService;

	@BeforeAll
	static void beforeAll() {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
	}

	@AfterAll
	static void afterAll() {
		if (POSTGRES.isRunning()) {
			POSTGRES.stop();
		}
	}

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		// Let Flyway manage the schema during tests
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		// Ensure Flyway runs and sees both main and test migrations
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db");
	}
}
