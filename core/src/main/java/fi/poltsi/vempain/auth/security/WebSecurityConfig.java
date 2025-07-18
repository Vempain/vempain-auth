package fi.poltsi.vempain.auth.security;

import fi.poltsi.vempain.auth.security.jwt.AuthTokenFilter;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	@Value("${vempain.cors.allowed-origins}")
	private List<String> allowedOrigins;
	@Value("${vempain.cors.max-age}")
	private long         maxAge;
	@Value("${vempain.cors.cors-pattern}")
	private String       corsPattern;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(withDefaults())
				.authorizeHttpRequests(auth -> {
					auth
							.requestMatchers("/login")
							.permitAll()
							.requestMatchers("/", "/index.html")
							.permitAll()
							.requestMatchers("/favicon.ico")
							.permitAll()
							.requestMatchers("/static/**")
							.permitAll()
							.requestMatchers("/img/**")
							.permitAll()
							.requestMatchers("/actuator/**")
							.permitAll()
							.requestMatchers("/v3/api-docs/**")
							.permitAll()
							.requestMatchers("/api/test/**") // This is only available in development environment
							.permitAll()
							.anyRequest()
							.authenticated();
				})
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
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
