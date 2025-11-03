package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestApp.class)
class UserAccountServiceITC extends IntegrationTestSetup {
	private static final Long count = 10L;

	@Test
	void findByIdOk() {
		var userId       = testITCTools.generateUser();
		var userOptional = userService.findById(userId);
		assertTrue(userOptional.isPresent());
		assertEquals(userId, userOptional.get()
										 .getId());
		userService.lockUser(userId);
	}

	@Test
	void findAllOk() {
		testITCTools.generateUsers(count);
		var users = userService.findAll();
		assertTrue(StreamSupport.stream(users.spliterator(), false)
								.count() >= count);
	}
}
