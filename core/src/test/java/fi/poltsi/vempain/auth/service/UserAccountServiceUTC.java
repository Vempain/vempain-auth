package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.tools.TestUTCTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceUTC {

	@Mock
	private UserAccountRepository userAccountRepository;

	@InjectMocks
	private UserService userService;

	@Test
	void findAllOk() {
		List<UserAccount> users = TestUTCTools.generateUserList(10L);
		when(userAccountRepository.findAll()).thenReturn(users);

		try {
			Iterable<UserAccount> returnValue = userService.findAll();
			assertNotNull(returnValue);
			assertEquals(10, StreamSupport.stream(returnValue.spliterator(), false)
										  .count());
		} catch (Exception e) {
			fail("We should not have received any exception");
		}
	}

	@Test
	void findByIdOk() {
		UserAccount userAccount = TestUTCTools.generateUser(ADMIN_ID);
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(userAccount));

		try {
			Optional<UserAccount> returnUser = userService.findById(1L);
			assertTrue(returnUser.isPresent());
			assertEquals(userAccount, returnUser.get());
		} catch (Exception e) {
			fail("We should not have received any exception");
		}
	}

	@Test
	void lockByIdOk() {
		doNothing().when(userAccountRepository)
				   .lockByUserId(1L);

		try {
			userService.lockUser(1L);
		} catch (Exception e) {
			fail("We should not have received any exception");
		}
	}
}
