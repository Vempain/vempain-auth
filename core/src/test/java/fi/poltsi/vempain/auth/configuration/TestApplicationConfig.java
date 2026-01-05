package fi.poltsi.vempain.auth.configuration;

import fi.poltsi.vempain.auth.security.WebSecurityConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tools.jackson.databind.ObjectMapper;

@EnableAutoConfiguration
@Import(WebSecurityConfig.class)
@ComponentScan(basePackages = "fi.poltsi.vempain.auth")
@EnableJpaRepositories(basePackages = "fi.poltsi.vempain.auth.repository")
@EntityScan(basePackages = "fi.poltsi.vempain.auth.entity")
public class TestApplicationConfig {
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
