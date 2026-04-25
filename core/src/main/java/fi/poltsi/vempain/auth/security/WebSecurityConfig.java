package fi.poltsi.vempain.auth.security;

import fi.poltsi.vempain.auth.security.jwt.AuthEntryPointJwt;
import fi.poltsi.vempain.auth.security.jwt.AuthTokenFilter;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
	private final UserDetailsServiceImpl userDetailsServiceImpl;
	private final AuthEntryPointJwt authEntryPointJwt;
	private final Environment       environment;

	@Value("${vempain.cors.allowed-origins}")
	private List<String> allowedOrigins;
	@Value("${vempain.cors.max-age}")
	private long         maxAge;
	@Value("${vempain.cors.cors-pattern}")
	private String       corsPattern;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		var isLocalProfileActive = environment.acceptsProfiles(Profiles.of("local"));

		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(withDefaults())
				.authorizeHttpRequests(auth -> {
					auth
							.dispatcherTypeMatchers(DispatcherType.ERROR)
							.permitAll()
							.requestMatchers("/error")
							.permitAll()
							.requestMatchers("/login")
							.permitAll()
							.requestMatchers("/actuator/health", "/actuator/health/**")
							.permitAll();

					if (isLocalProfileActive) {
						auth
								.requestMatchers("/swagger-ui.html", "/swagger-ui/**")
								.permitAll()
								.requestMatchers("/v3/api-docs/**")
								.permitAll()
								.requestMatchers("/actuator/**")
								.permitAll()
								.requestMatchers("/api/test/**")
								.permitAll();
					} else {
						auth
								.requestMatchers("/swagger-ui.html", "/swagger-ui/**")
								.denyAll()
								.requestMatchers("/v3/api-docs/**")
								.denyAll()
								.requestMatchers("/actuator/**")
								.denyAll();
					}

					auth
							.anyRequest()
							.authenticated();
				})
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(authEntryPointJwt)
						.accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN,
						                                                                                      "Error: Forbidden")))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
		;
		return http.build();
	}

	@Bean(name = "corsConfigurationSource")
	public CorsConfigurationSource corsConfigurationSource() {
		log.debug("Configuring CORS with allowed origins {}, maxAge {} and corsPattern {}", allowedOrigins, maxAge, corsPattern);
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(Collections.singletonList("*"));
		configuration.setAllowedHeaders(Collections.singletonList("*"));
		configuration.setMaxAge(maxAge);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration(corsPattern, configuration);
		return source;
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsServiceImpl);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
		return authConfiguration.getAuthenticationManager();
	}

	@Bean
	public AuthTokenFilter authenticationJwtTokenFilter() {
		return new AuthTokenFilter();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}
}
