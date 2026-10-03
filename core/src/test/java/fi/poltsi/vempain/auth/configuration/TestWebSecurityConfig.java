package fi.poltsi.vempain.auth.configuration;

import fi.poltsi.vempain.auth.security.WebSecurityConfig;
import fi.poltsi.vempain.auth.security.jwt.AuthEntryPointJwt;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class TestWebSecurityConfig extends WebSecurityConfig {

	public TestWebSecurityConfig(UserDetailsServiceImpl userDetailsServiceImpl, AuthEntryPointJwt authEntryPointJwt, Environment environment) {
		super(userDetailsServiceImpl, authEntryPointJwt, environment);
	}

	@Override
	protected void configureApplicationAuthorization(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
	}
}
