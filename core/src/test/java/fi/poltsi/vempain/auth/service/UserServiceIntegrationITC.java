package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = fi.poltsi.vempain.auth.TestApp.class)
class UserServiceIntegrationITC extends IntegrationTestSetup {

	@Autowired
	private UserService           userService;
	@Autowired
	private UserAccountRepository userAccountRepository;

	@Test
	void lockUser_setsLockedTrue() {
		var user = UserAccount.builder()
							  .loginName("lockme")
							  .nick("Lock Me")
							  .email("lock.me@example.com")
							  .password("$2a$12$abcdefghijklmnopqrstuv")
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

		user = userAccountRepository.save(user);

		userService.lockUser(user.getId());

		var reloaded = userAccountRepository.findById(user.getId())
											.orElseThrow();
		assertTrue(reloaded.isLocked());
	}
}
