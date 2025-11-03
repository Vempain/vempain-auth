package fi.poltsi.vempain.auth;

import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.service.AclService;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.auth.tools.TestITCTools;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@ActiveProfiles("test")
public abstract class IntegrationTestSetup {

	private static final PostgreSQLContainer<?> POSTGRES =
			new PostgreSQLContainer<>("postgres:16-alpine")
					.withDatabaseName("vempain_auth_test")
					.withUsername("test")
					.withPassword("test");

	// Start once for the whole test JVM; do not stop between classes
	static {
		if (!POSTGRES.isRunning()) {
			POSTGRES.start();
		}
	}

	@Autowired
	protected TestITCTools          testITCTools;
	// Services
	@Autowired
	protected AclService            aclService;
	@Autowired
	protected UnitService           unitService;
	@Autowired
	protected UserService           userService;
	// Repositories
	@Autowired
	protected AclRepository         aclRepository;
	@Autowired
	protected UnitRepository        unitRepository;
	@Autowired
	protected UserAccountRepository userAccountRepository;
	@Autowired
	private   EntityManager         entityManager;
	@Autowired
	private   DataSource            dataSource;

	@DynamicPropertySource
	static void datasourceProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		// Keep pools small for tests
		registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
		// Let Flyway manage the schema during tests
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		// Ensure Flyway runs and sees migrations
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.locations", () -> "classpath:db/migration/auth");
	}

	// Reset DB state before each test while keeping the Flyway-seeded admin (id=1).
	// Also ensure an ACL anchor row exists for admin so nextAclId() does not return 1.
	@BeforeEach
	void resetDatabase() throws Exception {
		try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
			// 1) Clear relations first
			s.execute("DELETE FROM user_unit");

			// 2) Remove all ACLs except a potential admin ACL row
			//    We will re-create the admin ACL row deterministically in step 5.
			s.execute("DELETE FROM acl");

			// 3) Clear units created by tests
			s.execute("DELETE FROM unit");

			// 4) Keep admin (id=1), remove any test-created users
			s.execute("DELETE FROM user_account WHERE id <> 1");

			// 5) Ensure admin ACL anchor exists in ACL table (acl_id from admin user_account)
			//    If Flyway hasn't created it, create one with permissive privileges.
			s.execute("""
							  	INSERT INTO acl (acl_id, user_id, unit_id, read_privilege, create_privilege, modify_privilege, delete_privilege)
							  	SELECT ua.acl_id, ua.id, NULL, TRUE, TRUE, TRUE, TRUE
							  	FROM user_account ua
							  	WHERE ua.id = 1
							  	  AND NOT EXISTS (SELECT 1 FROM acl a WHERE a.acl_id = ua.acl_id)
							  """);
		}
		// Clear the persistence context to avoid stale entities
		entityManager.clear();
	}
}
