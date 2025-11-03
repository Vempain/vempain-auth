package fi.poltsi.vempain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.request.LoginRequest;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LoginRTC extends IntegrationTestSetup {

	@Autowired
	private MockMvc         mockMvc;
	@Autowired
	private ObjectMapper    objectMapper;
	@Autowired
	private PasswordEncoder       passwordEncoder;
	@Autowired
	private UserAccountRepository userAccountRepository;
	@Autowired
	private UnitRepository        unitRepository;
	@MockitoBean
	private JwtUtils        jwtUtils;

	@BeforeEach
	void setup() {
		var unitAclId = testITCTools.generateAcl(ADMIN_ID, null, true, true, true, true);
		Unit unit = Unit.builder()
						.name("USER")
						// add required audit fields
						.creator(ADMIN_ID)
						.created(Instant.now())
						// if Unit extends AbstractVempainEntity, aclId must be positive
						.aclId(unitAclId)
						.build();
		unit = unitRepository.save(unit);
		var userAclId = testITCTools.generateAcl(ADMIN_ID, null, true, true, true, true);

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
							  .aclId(userAclId)
							  .units(Set.of(unit))
							  .creator(ADMIN_ID)
							  .created(Instant.now())
							  .build();
		userAccountRepository.save(user);

		Mockito.when(jwtUtils.generateJwtToken(any()))
			   .thenReturn("test.jwt.token");
	}

	@Test
	void loginOk() throws Exception {
		var req = LoginRequest.builder()
							  .login("testuser")
							  .password("S3cure-Pass!")
							  .build();

		mockMvc.perform(
					   post("/login")
							   .contentType(MediaType.APPLICATION_JSON)
							   .content(objectMapper.writeValueAsString(req)))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.token", notNullValue()))
			   .andExpect(jsonPath("$.login").value("testuser"));
	}
}
