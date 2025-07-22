package fi.poltsi.vempain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.request.LoginRequest;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UserRepository;
import fi.poltsi.vempain.auth.tools.AuthTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = fi.poltsi.vempain.auth.configuration.TestApplicationConfig.class)
@AutoConfigureMockMvc
@Testcontainers
public class LoginControllerITC {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
			.withDatabaseName("testdb")
			.withUsername("testuser")
			.withPassword("testpass");

	@DynamicPropertySource
	static void overrideProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AclRepository aclRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		if (userRepository.findByLoginName("testuser1")
						  .isEmpty()) {
			// Create related ACL with all permissions set to true
			var acl = fi.poltsi.vempain.auth.entity.Acl.builder()
													   .userId(1L)
													   .unitId(null)
													   .createPrivilege(true)
													   .readPrivilege(true)
													   .modifyPrivilege(true)
													   .deletePrivilege(true)
													   .build();
			// Inject and use AclRepository to save the ACL
			var newAcl = aclRepository.save(acl);

			var user = UserAccount.builder()
								  .loginName("testuser1")
								  .password(AuthTools.passwordHash("ValidPass123!"))
								  .name("Test User")
								  .nick("testnick1")
								  .email("testuser1@example.com")
								  .aclId(newAcl.getAclId())
								  .isPublic(false)
								  .privacyType(PrivacyType.PRIVATE)
								  .birthday(Instant.parse("2000-01-01T00:00:00Z"))
								  .status(AccountStatus.ACTIVE)
								  .locked(false)
								  .creator(1L)
								  .created(Instant.now())
								  .build();
			user = userRepository.save(user);

			user.setCreator(user.getId());
			userRepository.save(user);
		}
	}

	@Test
	void loginSucceedsWithCorrectCredentials() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setLogin("testuser1");
		request.setPassword("ValidPass123!");

		mockMvc.perform(post("/login")
								.contentType("application/json")
								.content(objectMapper.writeValueAsString(request)))
			   .andExpect(status().isOk());
	}

	@Test
	void loginFailsWithWrongPassword() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setLogin("testuser1");
		request.setPassword("WrongPassword!");

		mockMvc.perform(post("/login")
								.contentType("application/json")
								.content(objectMapper.writeValueAsString(request)))
			   .andExpect(status().isNotFound());
	}

	@Test
	void loginFailsWithNonexistentUser() throws Exception {
		LoginRequest request = new LoginRequest();
		request.setLogin("nonexistent");
		request.setPassword("AnyPassword123!");

		mockMvc.perform(post("/login")
								.contentType("application/json")
								.content(objectMapper.writeValueAsString(request)))
			   .andExpect(status().isNotFound());
	}
}
