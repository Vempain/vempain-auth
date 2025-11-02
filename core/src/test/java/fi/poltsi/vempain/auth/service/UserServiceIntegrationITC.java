package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UserServiceIntegrationITC.TestApp.class)
class UserServiceIntegrationITC extends IntegrationTestSetup {

	@Autowired
	private UserService    userService;
	@Autowired
	private UserRepository userRepository;

	@Test
	void lockUser_setsLockedTrue() {
		var user = UserAccount.builder()
							  .loginName("lockme")
							  .nick("Lock Me")
							  .email("lock.me@example.com")
							  .password("$2a$12$abcdefghijklmnopqrstuv") // dummy hash
							  .birthday(Instant.now()
											   .minus(100000, java.time.temporal.ChronoUnit.DAYS))
							  .name("testuser")
							  .privacyType(PrivacyType.PUBLIC)
							  .status(AccountStatus.ACTIVE)
							  .locked(false)
							  .aclId(123L)
							  .creator(1L)
							  .created(Instant.now())
							  .build();

		user = userRepository.save(user);

		userService.lockUser(user.getId());

		var reloaded = userRepository.findById(user.getId())
									 .orElseThrow();
		assertThat(reloaded.isLocked()).isTrue();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableJpaRepositories(basePackages = "fi.poltsi.vempain.auth.repository")
	@EntityScan(basePackages = "fi.poltsi.vempain.auth.entity")
	@ComponentScan(basePackages = {
			"fi.poltsi.vempain.auth.service",
			"fi.poltsi.vempain.auth.repository"
	})
	static class TestApp {
	}
}
