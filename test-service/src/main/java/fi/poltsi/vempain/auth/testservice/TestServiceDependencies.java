package fi.poltsi.vempain.auth.testservice;

import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.security.AclAuthorizationService;
import fi.poltsi.vempain.auth.security.jwt.AuthEntryPointJwt;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestServiceDependencies {

	@Bean
	UserDetailsServiceImpl userDetailsService() {
		return mock(UserDetailsServiceImpl.class);
	}

	@Bean
	AuthEntryPointJwt authEntryPointJwt() {
		return new AuthEntryPointJwt();
	}

	@Bean
	AclRepository aclRepository() {
		return mock(AclRepository.class);
	}

	@Bean("aclAuthorizationService")
	AclAuthorizationService aclAuthorizationService(AclRepository aclRepository) {
		return new AclAuthorizationService(aclRepository);
	}
}
