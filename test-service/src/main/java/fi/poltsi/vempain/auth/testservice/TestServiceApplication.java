package fi.poltsi.vempain.auth.testservice;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({TestServiceSecurityConfig.class, TestServiceController.class})
public class TestServiceApplication {
}
