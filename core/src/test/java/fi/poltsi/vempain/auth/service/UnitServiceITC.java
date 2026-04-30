package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest(classes = TestApp.class)
class UnitServiceITC extends IntegrationTestSetup {

	@Autowired
	private UnitService unitService;

	@Test
	void findAllOk() {
		testITCTools.generateUnit();
		var units = unitService.findAll();
		assertNotNull(units);
		var count = 0;
		for (var ignored : units) {
			count++;
		}
		assertTrue(count > 0);
	}

	@Test
	void findByIdOk() {
		var unitId = testITCTools.generateUnit();

		try {
			var response = unitService.findById(unitId);
			assertNotNull(response);
			assertEquals(unitId, response.getId());
		} catch (VempainEntityNotFoundException e) {
			fail("Should not have thrown for a valid unit ID: " + e.getMessage());
		}
	}

	@Test
	void findByIdNotFoundThrows() {
		try {
			unitService.findById(Long.MAX_VALUE);
			fail("Should have thrown VempainEntityNotFoundException");
		} catch (VempainEntityNotFoundException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	void createUnitOk() {
		var adminAclId = testITCTools.generateAcl(ADMIN_ID, null, true, true, true, true);
		var req = UnitRequest.builder()
							 .name("NewITCUnit_" + System.currentTimeMillis())
							 .description("Integration test unit")
							 .acls(List.of(AclRequest.builder()
												  .aclId(adminAclId)
												  .user(ADMIN_ID)
												  .unit(null)
												  .readPrivilege(true)
												  .createPrivilege(true)
												  .modifyPrivilege(true)
												  .deletePrivilege(true)
												  .build()))
							 .build();

		// createUnit needs a SecurityContext with the current user
		setSecurityContext(ADMIN_ID);
		try {
			var response = unitService.createUnit(req);
			assertNotNull(response);
			assertNotNull(response.getName());
		} finally {
			org.springframework.security.core.context.SecurityContextHolder.clearContext();
		}
	}

	@Test
	void deleteByIdOk() {
		var unitId = testITCTools.generateUnit();
		assertNotNull(unitId);

		unitService.deleteById(unitId);

		try {
			unitService.findById(unitId);
			fail("Unit should have been deleted");
		} catch (VempainEntityNotFoundException e) {
			assertNotNull(e.getMessage());
		}
	}

	@Test
	void saveUnitOk() {
		var userId = testITCTools.generateUser();
		var aclId  = testITCTools.generateAcl(userId, null, true, true, true, true);

		var unit = fi.poltsi.vempain.auth.entity.Unit.builder()
													  .name("SavedITCUnit_" + System.currentTimeMillis())
													  .description("Saved integration unit")
													  .aclId(aclId)
													  .creator(userId)
													  .created(java.time.Instant.now())
													  .build();

		var saved = unitService.save(unit);
		assertNotNull(saved);
		assertNotNull(saved.getId());
	}

	// ── helpers ───────────────────────────────────────────────────────────────────

	private void setSecurityContext(long userId) {
		var userDetails = new UserDetailsImpl(userId, "login", "nick", "email@test.com",
				"password", java.util.Set.of(), null);
		var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null);
		org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
	}
}
