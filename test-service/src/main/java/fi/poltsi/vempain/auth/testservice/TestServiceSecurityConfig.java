package fi.poltsi.vempain.auth.testservice;

import fi.poltsi.vempain.auth.security.WebSecurityConfig;
import fi.poltsi.vempain.auth.security.jwt.AuthEntryPointJwt;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import({TestServiceDependencies.class, JwtUtils.class})
public class TestServiceSecurityConfig extends WebSecurityConfig {

	public TestServiceSecurityConfig(UserDetailsServiceImpl userDetailsServiceImpl, AuthEntryPointJwt authEntryPointJwt,
									 Environment environment) {
		super(userDetailsServiceImpl, authEntryPointJwt, environment);
	}

	@Override
	protected void configureApplicationAuthorization(ApplicationAuthorizationConfigurer authorization) {
		authorization.authenticated(HttpMethod.GET, "/test-service/protected");
		authorization.hasRole("ADMIN", HttpMethod.POST, "/test-service/admin");
	}
}
