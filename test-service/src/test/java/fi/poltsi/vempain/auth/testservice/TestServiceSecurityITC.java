package fi.poltsi.vempain.auth.testservice;

import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.security.jwt.JwtToken;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.reset;
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

	@Autowired
	private AclRepository aclRepository;

	@org.junit.jupiter.api.BeforeEach
	void resetAclState() {
		reset(aclRepository);
	}

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
	void authenticatedUserWithoutAclCannotAccessResource() throws Exception {
		when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails("user"));
		mockMvc.perform(get("/test-service/resource/42").header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isForbidden());
	}

	@Test
	void userAclControlsResourceRead() throws Exception {
		when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails("user"));
		when(aclRepository.getAclByAclId(42L)).thenReturn(java.util.List.of(Acl.builder()
																			   .aclId(42L)
																			   .userId(1L)
																			   .readPrivilege(true)
																			   .build()));

		mockMvc.perform(get("/test-service/resource/42").header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isOk());
	}

	@Test
	void aclForDifferentUserCannotAccessResource() throws Exception {
		when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails(1L, "user"));
		when(aclRepository.getAclByAclId(42L)).thenReturn(java.util.List.of(Acl.builder()
																			   .aclId(42L)
																			   .userId(2L)
																			   .readPrivilege(true)
																			   .build()));

		mockMvc.perform(get("/test-service/resource/42").header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isForbidden());
	}

	@Test
	void unitAclControlsResourceRead() throws Exception {
		when(userDetailsService.loadUserByUsername("unit-user")).thenReturn(userDetails("unit-user",
																						Set.of(Unit.builder()
		                                                                                           .id(7L)
		                                                                                           .build())));
		when(aclRepository.getAclByAclId(42L)).thenReturn(java.util.List.of(Acl.builder()
																			   .aclId(42L)
																			   .unitId(7L)
																			   .readPrivilege(true)
																			   .build()));

		mockMvc.perform(get("/test-service/resource/42").header("Authorization", "Bearer " + tokenFor("unit-user")))
			   .andExpect(status().isOk());
	}

	@Test
	void aclForUnrelatedUnitCannotAccessResource() throws Exception {
		when(userDetailsService.loadUserByUsername("unit-user")).thenReturn(userDetails("unit-user",
																						Set.of(Unit.builder()
		                                                                                           .id(7L)
		                                                                                           .build())));
		when(aclRepository.getAclByAclId(42L)).thenReturn(java.util.List.of(Acl.builder()
																			   .aclId(42L)
																			   .unitId(8L)
																			   .readPrivilege(true)
																			   .build()));

		mockMvc.perform(get("/test-service/resource/42").header("Authorization", "Bearer " + tokenFor("unit-user")))
			   .andExpect(status().isForbidden());
	}

	@Test
	void eachResourceOperationUsesItsOwnAclPrivilege() throws Exception {
		when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails("user"));
		when(aclRepository.getAclByAclId(42L)).thenReturn(java.util.List.of(Acl.builder()
																			   .aclId(42L)
																			   .userId(1L)
																			   .createPrivilege(true)
																			   .modifyPrivilege(false)
																			   .deletePrivilege(false)
																			   .build()));

		mockMvc.perform(post("/test-service/resource/42").header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isOk());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/test-service/resource/42")
																						   .header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isForbidden());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/test-service/resource/42")
																						   .header("Authorization", "Bearer " + tokenFor("user")))
			   .andExpect(status().isForbidden());
	}

	private String tokenFor(String username) {
		JwtToken token = jwtUtils.generateJwtTokenForUser(username, username, username + "@example.test");
		return token.getTokenString();
	}

	private UserDetailsImpl userDetails(String username) {
		return userDetails(1L, username, Set.of());
	}

	private UserDetailsImpl userDetails(String username, Set<Unit> units) {
		return userDetails(1L, username, units);
	}

	private UserDetailsImpl userDetails(Long id, String username) {
		return userDetails(id, username, Set.of());
	}

	private UserDetailsImpl userDetails(Long id, String username, Set<Unit> units) {
		return new UserDetailsImpl(id, username, username, username + "@example.test", "password", units, Set.of());
	}
}
