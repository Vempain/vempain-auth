package fi.poltsi.vempain.auth.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.request.LoginRequest;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LoginIntegrationITC.TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LoginIntegrationITC extends BasePostgresContainerSetup {

	@Autowired
	private MockMvc         mockMvc;
	@Autowired
	private ObjectMapper    objectMapper;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private UserRepository  userRepository;
	@Autowired
	private UnitRepository  unitRepository;
	@MockitoBean
	private JwtUtils        jwtUtils;

	@BeforeEach
	void setup() {
		userRepository.deleteAll();
		unitRepository.deleteAll();

		Unit unit = Unit.builder()
						.name("USER")
						// add required audit fields
						.creator(1L)
						.created(Instant.now())
						// if Unit extends AbstractVempainEntity, aclId must be positive
						.aclId(1L)
						.build();
		unit = unitRepository.save(unit);

		var user = UserAccount.builder()
							  .loginName("testuser")
							  .nick("Testy")
							  .email("test@example.com")
							  .password(passwordEncoder.encode("S3cure-Pass!"))
							  .birthday(Instant.now()
											   .minus(100000, java.time.temporal.ChronoUnit.DAYS))
							  .name("testuser")
							  .privacyType(PrivacyType.PUBLIC)
							  .status(AccountStatus.ACTIVE)
							  .locked(false)
							  .aclId(1L)
							  .units(Set.of(unit))
							  .creator(1L)
							  .created(Instant.now())
							  .build();
		userRepository.save(user);

		Mockito.when(jwtUtils.generateJwtToken(any()))
			   .thenReturn("test.jwt.token");
	}

	@Test
	void login_succeeds_and_returns_token() throws Exception {
		var req = new LoginRequest();
		req.setLogin("testuser");
		req.setPassword("S3cure-Pass!");

		mockMvc.perform(
					   post("/login")
							   .contentType(MediaType.APPLICATION_JSON)
							   .content(objectMapper.writeValueAsString(req)))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.token", notNullValue()))
			   .andExpect(jsonPath("$.login").value("testuser"));
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableJpaRepositories(basePackages = "fi.poltsi.vempain.auth.repository")
	@EntityScan(basePackages = "fi.poltsi.vempain.auth.entity")
	@ComponentScan(basePackages = "fi.poltsi.vempain.auth")
	static class TestApp {
	}
}
