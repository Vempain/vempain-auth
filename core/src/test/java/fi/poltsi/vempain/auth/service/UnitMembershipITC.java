package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import fi.poltsi.vempain.auth.entity.Unit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Nested unit memberships against the real schema: members are stored and read back, cycles are rejected, and the principal of a user
 * carries every unit containing its direct units.
 */
@SpringBootTest(classes = TestApp.class)
class UnitMembershipITC extends IntegrationTestSetup {
	@Autowired
	private UserDetailsServiceImpl userDetailsService;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private long unit(String name) {
		var aclId = testITCTools.generateAcl(ADMIN_ID, null, true, true, true, true);
		return unitService.save(Unit.builder()
									.name(name)
									.description("ITC " + name)
									.aclId(aclId)
									.locked(false)
									.creator(ADMIN_ID)
									.created(Instant.now())
									.build())
						  .getId();
	}

	private static AclRequest adminAcl() {
		return AclRequest.builder()
						 .user(ADMIN_ID)
						 .readPrivilege(true)
						 .createPrivilege(true)
						 .modifyPrivilege(true)
						 .deletePrivilege(true)
						 .build();
	}

	private void runAsAdmin() {
		var admin = userAccountRepository.findById(ADMIN_ID)
										 .orElseThrow();
		var principal = UserDetailsImpl.build(admin);
		SecurityContextHolder.getContext()
							 .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@Test
	void membersAreStoredReadBackAndReplaced() throws Exception {
		runAsAdmin();
		var userId      = testITCTools.generateUser();
		var otherUserId = testITCTools.generateUser();
		var child       = unit("child");
		var other       = unit("other");

		var created = unitService.createUnit(UnitRequest.builder()
														.name("parent")
														.description("has members")
														.acls(List.of(adminAcl()))
														.userIds(List.of(userId))
														.unitIds(List.of(child))
														.build());

		assertEquals(List.of(userId), created.getUserIds());
		assertEquals(List.of(child), created.getUnitIds());
		assertEquals(List.of(userId), unitService.findById(created.getId())
		                                         .getUserIds());
		assertTrue(userService.findUserResponseById(userId)
		                      .getUnitIds()
		                      .contains(created.getId()));

		// Replacing the members drops the old ones
		var updated = unitService.updateUnit(created.getId(), UnitRequest.builder()
																		 .name("parent")
																		 .description("replaced")
																		 .acls(List.of(adminAcl()))
																		 .userIds(List.of(otherUserId))
																		 .unitIds(List.of(other))
																		 .build());
		assertEquals(List.of(otherUserId), updated.getUserIds());
		assertEquals(List.of(other), updated.getUnitIds());
		assertTrue(userService.findUserResponseById(userId)
		                      .getUnitIds()
		                      .stream()
		                      .noneMatch(id -> id.equals(created.getId())));

		// Null lists leave the memberships untouched
		var untouched = unitService.updateUnit(created.getId(), UnitRequest.builder()
																		   .name("parent")
																		   .description("untouched")
																		   .acls(List.of(adminAcl()))
																		   .build());
		assertEquals(List.of(otherUserId), untouched.getUserIds());
		assertEquals(List.of(other), untouched.getUnitIds());
	}

	@Test
	void circularAndSelfMembershipsAreRejectedBeforeAnythingIsStored() throws Exception {
		runAsAdmin();
		var a = unit("A");
		var b = unit("B");
		var c = unit("C");
		unitService.updateMembers(a, null, List.of(b));
		unitService.updateMembers(b, null, List.of(c));

		// C may not contain A: A contains B contains C
		var cycle = assertThrows(ResponseStatusException.class, () -> unitService.updateMembers(c, null, List.of(a)));
		assertEquals(HttpStatus.BAD_REQUEST, cycle.getStatusCode());
		assertEquals(List.of(), unitService.findById(c)
		                                   .getUnitIds());

		var self = assertThrows(ResponseStatusException.class, () -> unitService.updateMembers(c, null, List.of(c)));
		assertEquals(HttpStatus.BAD_REQUEST, self.getStatusCode());

		var direct = assertThrows(ResponseStatusException.class, () -> unitService.updateMembers(b, null, List.of(a)));
		assertEquals(HttpStatus.BAD_REQUEST, direct.getStatusCode());
		assertEquals(List.of(c), unitService.findById(b)
		                                    .getUnitIds(), "the rejected update left the members as they were");

		var unknown = assertThrows(ResponseStatusException.class, () -> unitService.updateMembers(c, null, List.of(987654L)));
		assertEquals(HttpStatus.BAD_REQUEST, unknown.getStatusCode());
		var unknownUser = assertThrows(ResponseStatusException.class, () -> unitService.updateMembers(c, List.of(987654L), null));
		assertEquals(HttpStatus.BAD_REQUEST, unknownUser.getStatusCode());
	}

	@Test
	void concurrentMembershipUpdatesCannotCreateACycle() throws Exception {
		var a = unit("A");
		var b = unit("B");
		var firstUpdateComplete = new CountDownLatch(1);
		var releaseFirstUpdate = new CountDownLatch(1);
		var secondUpdateStarted = new CountDownLatch(1);
		var executor = Executors.newFixedThreadPool(2);

		try {
			var firstUpdate = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
				unitService.updateMembers(a, null, List.of(b));
				firstUpdateComplete.countDown();
				await(releaseFirstUpdate);
			}));
			assertTrue(firstUpdateComplete.await(5, TimeUnit.SECONDS));

			var secondUpdate = executor.submit(() -> {
				secondUpdateStarted.countDown();
				unitService.updateMembers(b, null, List.of(a));
			});
			assertTrue(secondUpdateStarted.await(5, TimeUnit.SECONDS));
			assertThrows(TimeoutException.class, () -> secondUpdate.get(200, TimeUnit.MILLISECONDS));

			releaseFirstUpdate.countDown();
			firstUpdate.get(5, TimeUnit.SECONDS);
			var exception = assertThrows(ExecutionException.class, () -> secondUpdate.get(5, TimeUnit.SECONDS));
			assertTrue(exception.getCause() instanceof ResponseStatusException);
			assertEquals(HttpStatus.BAD_REQUEST, ((ResponseStatusException) exception.getCause()).getStatusCode());
		} finally {
			releaseFirstUpdate.countDown();
			executor.shutdownNow();
		}

		assertFalse(unitService.findById(b).getUnitIds().contains(a));
	}

	@Test
	void principalCarriesEveryContainingUnit() {
		runAsAdmin();
		var a = unit("A");
		var b = unit("B");
		var c = unit("C");
		unitService.updateMembers(a, null, List.of(b));
		unitService.updateMembers(b, null, List.of(c));

		var created = userService.createUser(UserRequest.builder()
														.loginName("nested.member")
														.name("Nested Member")
														.nick("nested")
														.email("nested@test.tld")
														.password("S3cure-Pass!")
														.privacyType(PrivacyType.PRIVATE)
														.birthday(Instant.parse("1990-01-01T00:00:00Z"))
														.acls(List.of(adminAcl()))
														.unitIds(List.of(c))
														.build());
		assertEquals(List.of(c), created.getUnitIds());

		var principal = (UserDetailsImpl) userDetailsService.loadUserByUsername("nested.member");
		var unitIds = principal.getUnits()
							   .stream()
							   .map(Unit::getId)
							   .collect(Collectors.toSet());

		assertEquals(Set.of(a, b, c), unitIds);
		// A user whose unit membership is replaced with an empty list ends up with no units
		var cleared = userService.updateUser(created.getId(), UserRequest.builder()
																		 .loginName("nested.member")
																		 .name("Nested Member")
																		 .nick("nested")
																		 .email("nested@test.tld")
																		 .privacyType(PrivacyType.PRIVATE)
																		 .birthday(Instant.parse("1990-01-01T00:00:00Z"))
																		 .acls(List.of(adminAcl()))
																		 .unitIds(List.of())
																		 .build());
		assertEquals(List.of(), cleared.getUnitIds());
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new AssertionError("Timed out waiting for the concurrent membership update test");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}
}
