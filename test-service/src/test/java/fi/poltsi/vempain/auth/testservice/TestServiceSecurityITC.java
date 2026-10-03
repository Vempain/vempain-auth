package fi.poltsi.vempain.auth.testservice;

import fi.poltsi.vempain.auth.security.jwt.JwtToken;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestServiceApplication.class, properties = {
		"vempain.cors.allowed-origins=*",
		"vempain.cors.max-age=3600",
		"vempain.cors.cors-pattern=/**",
		"vempain.app.jwt-secret=test-secret",
		"vempain.app.jwt-expiration-ms=3600000",
		"spring.autoconfigure.exclude=" +
		"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
		"org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
		"org.springframework.boot.jpa.autoconfigure.HibernateJpaAutoConfiguration"
})
@AutoConfigureMockMvc
class TestServiceSecurityITC {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtUtils jwtUtils;

	@Autowired
	private UserDetailsService userDetailsService;

	@Test
	void protectedApplicationEndpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/test-service/protected"))
			   .andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanAccessAuthenticatedApplicationEndpoint() throws Exception {
		when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails("user"));
		mockMvc.perform(get("/test-service/protected").header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isOk());
	}

	@Test
	void nonAdminUserCannotAccessAdminApplicationEndpoint() throws Exception {
		when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails("user"));
		mockMvc.perform(post("/test-service/admin").header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isForbidden());
	}

	@Test
	void adminUserCanAccessAdminApplicationEndpoint() throws Exception {
		when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails("admin", "ROLE_ADMIN"));
		mockMvc.perform(post("/test-service/admin").header("Authorization", "Bearer " + tokenFor("admin")))
			   .andExpect(status().isOk());
	}

	private String tokenFor(String username) {
		JwtToken token = jwtUtils.generateJwtTokenForUser(username, username, username + "@example.test");
		return token.getTokenString();
	}

	private UserDetailsImpl userDetails(String username, String... authorities) {
		return new UserDetailsImpl(1L, username, username, username + "@example.test", "password", Set.of(),
								   Arrays.stream(authorities)
		                                 .map(SimpleGrantedAuthority::new)
		                                 .toList());
	}
}
