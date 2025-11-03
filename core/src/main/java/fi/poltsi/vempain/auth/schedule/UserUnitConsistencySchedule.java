package fi.poltsi.vempain.auth.schedule;

import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.service.UnitService;
import fi.poltsi.vempain.auth.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@AllArgsConstructor
@Service
public class UserUnitConsistencySchedule {
	private static final long   DELAY         = 60 * 60 * 1000L;
	private static final String INITIAL_DELAY = "#{ 30 * 1000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(" + DELAY + ") }";

	private final UnitService unitService;
	private final UserService userService;

	@Scheduled(fixedDelay = DELAY, initialDelayString = INITIAL_DELAY)
	public void verify() {
		Iterable<UserAccount> users          = userService.findAll();
		Iterable<Unit>        units          = unitService.findAll();
		Set<Unit>             unitsWithUsers = new HashSet<>();

		for (UserAccount userAccount : users) {
			if (userAccount.getUnits()
						   .isEmpty()) {
				log.error("User ID {} ({}) does not belong to any unit", userAccount.getId(), userAccount.getName());
			} else {
				unitsWithUsers.addAll(userAccount.getUnits());
			}
		}

		for (Unit unit : units) {
			if (!unitsWithUsers.contains(unit)) {
				log.warn("Unit ID {} ({}) does not have any users", unit.getId(), unit.getName());
			}
		}
	}
}
