package fi.poltsi.vempain.auth;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "fi.poltsi.vempain.auth.repository")
@EntityScan(basePackages = "fi.poltsi.vempain.auth.entity")
@ComponentScan(basePackages = "fi.poltsi.vempain.auth")
public class TestApp {
}
