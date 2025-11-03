package fi.poltsi.vempain.auth.schedule;

import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;
import fi.poltsi.vempain.auth.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

class UserAccountUnitConsistencyScheduleUTC {
	@Mock
	UnitService unitService;
	@Mock
	UserService userService;

	private UserUnitConsistencySchedule userUnitConsistencySchedule;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		userUnitConsistencySchedule = new UserUnitConsistencySchedule(unitService, userService);
	}

	@Test
	void verifyOk() {
		List<UserAccount> users = TestUTCTools.generateUserList(5L);
		List<Unit>        units = TestUTCTools.generateUnitList(5L);

		// We add a user with non-empty unit list
		users.getFirst()
			 .setUnits(Collections.singleton(units.getFirst()));

		when(userService.findAll()).thenReturn(users);
		when(unitService.findAll()).thenReturn(units);

		try {
			userUnitConsistencySchedule.verify();
		} catch (Exception e) {
			fail("Should not have received any exceptions for normal run: " + e);
		}
	}
}
