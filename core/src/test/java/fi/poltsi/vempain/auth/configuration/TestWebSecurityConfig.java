package fi.poltsi.vempain.auth.configuration;

import fi.poltsi.vempain.auth.security.WebSecurityConfig;
import fi.poltsi.vempain.auth.security.jwt.AuthEntryPointJwt;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * The host-application security configuration of the core tests. It uses every extension point of {@link WebSecurityConfig} the way a
 * backend does: both {@code authenticated} overloads of the application rules and an additional authentication filter in front of the
 * JWT filter. The filter only marks the response with {@link #ADDITIONAL_FILTER_HEADER}, so it never changes the outcome of a request.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class TestWebSecurityConfig extends WebSecurityConfig {
	public static final String ADDITIONAL_FILTER_HEADER = "X-Vempain-Test-Filter";
	public static final String PATTERN_RULE_PATH        = "/test-security/pattern";
	public static final String METHOD_RULE_PATH         = "/test-security/method";

	public TestWebSecurityConfig(UserDetailsServiceImpl userDetailsServiceImpl, AuthEntryPointJwt authEntryPointJwt, Environment environment) {
		super(userDetailsServiceImpl, authEntryPointJwt, environment);
	}

	@Override
	protected void configureApplicationAuthorization(ApplicationAuthorizationConfigurer authorization) {
		authorization.authenticated(PATTERN_RULE_PATH + "/**");
		authorization.authenticated(HttpMethod.POST, METHOD_RULE_PATH + "/**");
	}

	@Override
	protected List<Filter> additionalAuthenticationFilters() {
		return List.of(new MarkerFilter());
	}

	/**
	 * Stands in for a host application's service-to-service authentication filter: it runs inside the security filter chain and
	 * leaves a trace in the response.
	 */
	private static class MarkerFilter extends OncePerRequestFilter {
		@Override
		protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
										@NonNull FilterChain filterChain) throws ServletException, IOException {
			response.setHeader(ADDITIONAL_FILTER_HEADER, "ran");
			filterChain.doFilter(request, response);
		}
	}
}
