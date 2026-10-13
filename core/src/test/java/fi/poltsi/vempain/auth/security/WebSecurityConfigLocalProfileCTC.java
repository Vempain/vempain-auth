package fi.poltsi.vempain.auth.security;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With the {@code local} profile active, {@link WebSecurityConfig} opens the Swagger, OpenAPI, actuator and {@code /api/test} paths to
 * anonymous callers (developer convenience); every other path still requires authentication.
 */
@SpringBootTest(classes = TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WebSecurityConfigLocalProfileCTC extends IntegrationTestSetup {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void developerPathsAreOpenToAnonymousCallers() throws Exception {
		// None of these paths has a handler in the library, so passing the security rules ends with 404 instead of 401
		mockMvc.perform(get("/swagger-ui.html"))
			   .andExpect(status().isNotFound());
		mockMvc.perform(get("/swagger-ui/index.html"))
			   .andExpect(status().isNotFound());
		mockMvc.perform(get("/v3/api-docs/public"))
			   .andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/prometheus"))
			   .andExpect(status().isNotFound());
		mockMvc.perform(get("/api/test/anything"))
			   .andExpect(status().isNotFound());
	}

	@Test
	void otherPathsStillRequireAuthentication() throws Exception {
		mockMvc.perform(get("/content-management/users"))
			   .andExpect(status().isUnauthorized());
	}
}
