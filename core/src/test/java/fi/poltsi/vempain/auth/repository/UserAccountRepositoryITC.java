package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.entity.UserAccount;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static fi.poltsi.vempain.auth.tools.TestUserAccountTools.encryptPassword;
import static fi.poltsi.vempain.auth.tools.TestUserAccountTools.randomPassword;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(classes = TestApp.class)
class UserAccountRepositoryITC extends IntegrationTestSetup {
	@Test
	void injectedComponentsAreNotNull() {
		assertNotNull(userAccountRepository);
	}

	@Test
	@Transactional
	void createAndLockUserOk() {
		var userId       = testITCTools.generateUser();
		var optionalUser = userAccountRepository.findById(userId);
		assertTrue(optionalUser.isPresent());
		var user = optionalUser.get();

		assertTrue(user.getAclId() > 0, "User ACL ID should have been > 0, now it is " + user.getAclId());
		assertEquals(userId, user.getCreator());
		assertNotNull(user.getModifier());
		assertNotNull(user.getModified());
		Assertions.assertFalse(user.isPublic());
		assertEquals(PrivacyType.PRIVATE, user.getPrivacyType());
		assertNotNull(user.getBirthday());
		assertNotNull(user.getCreated());
		assertNotNull(user.getDescription());
		assertNotNull(user.getLoginName());
		assertNotNull(user.getModified());
		assertFalse(user.getPob()
						.isEmpty(), "User POB length should have been > 0, now it is empty");

		// Lock user
		String loginName = user.getLoginName();
		user.setLocked(true);
		userAccountRepository.save(user);
		var optionalLockedUser = userAccountRepository.findByLoginName(loginName);
		assertTrue(optionalLockedUser.isPresent(), "The user should have been found by login name");
		var lockedUser = optionalLockedUser.get();
		// TimeUnit.MINUTES.sleep(10);
		assertTrue(lockedUser.isLocked(), "The user should have been locked");
	}

	@Transactional
	@Test
	void failUserCreationMissingNick() {
		var password = randomPassword(15);
		var userAccount = UserAccount.builder()
									 .aclId(1L)
									 .birthday(Instant.now()
													  .minus(20 * 365, ChronoUnit.DAYS))
									 .created(Instant.now()
													 .minus(1, ChronoUnit.HOURS))
									 .creator(1L)
									 .description("ITC generated user " + password)
									 .email("first." + password + "@test.tld")
									 .id(1L)
									 .locked(false)
									 .loginName(password)
									 .modified(Instant.now())
									 .modifier(1L)
									 .name("Firstname " + password)
									 .password(encryptPassword(password))
									 .pob("1111")
									 .privacyType(PrivacyType.PRIVATE)
									 .isPublic(false)
									 .street("")
									 .units(null)
									 .build();
		DataIntegrityViolationException dive =
				assertThrows(DataIntegrityViolationException.class,
							 () -> {
								 // When we have these auxiliary lines of code here, the test works
								 log.info("Creating user account: {}", userAccount); // Aux
								 var newUser = userAccountRepository.save(userAccount);
								 log.info("newUser: {}", newUser); // Aux
								 var users = userAccountRepository.findAll(); // Aux
								 log.info("users: {}", users); // Aux
							 },
							 "Expected SQLIntegrityConstraintViolationException because of incomplete user information");
		assertNotNull(dive);
		assertNotNull(dive.getMessage());
		log.info("dive.getMessage: {}", dive.getMessage());
		assertTrue(dive.getMessage()
					   .contains("not-null property references a null or transient value for entity fi.poltsi.vempain.auth.entity.UserAccount.nick"));
	}
}
