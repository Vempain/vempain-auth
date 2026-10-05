package fi.poltsi.vempain.auth.security;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.security.AclAuthorizationService.AclPrivilege;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link AclAuthorizationService} against ACL rows stored in PostgreSQL, so that the
 * repository query and the privilege evaluation are tested together.
 */
@SpringBootTest(classes = TestApp.class)
class AclAuthorizationServiceITC extends IntegrationTestSetup {

	@Autowired
	private AclAuthorizationService aclAuthorizationService;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void userAclGrantsExactlyTheStoredPrivileges() {
		var userId = testITCTools.generateUser();
		var aclId  = testITCTools.generateAcl(userId, null, true, true, false, false);
		authenticate(userId, Set.of());

		assertTrue(aclAuthorizationService.canRead(aclId));
		assertTrue(aclAuthorizationService.canModify(aclId));
		assertFalse(aclAuthorizationService.canCreate(aclId));
		assertFalse(aclAuthorizationService.canDelete(aclId));
	}

	@Test
	void aclOfAnotherUserIsDenied() {
		var ownerId = testITCTools.generateUser();
		var otherId = testITCTools.generateUser();
		var aclId   = testITCTools.generateAcl(ownerId, null, true, true, true, true);
		authenticate(otherId, Set.of());

		assertFalse(aclAuthorizationService.canRead(aclId));
		assertFalse(aclAuthorizationService.canModify(aclId));
		assertFalse(aclAuthorizationService.canCreate(aclId));
		assertFalse(aclAuthorizationService.canDelete(aclId));
	}

	@Test
	void unitAclGrantsAccessToUnitMembers() {
		var userId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		var aclId  = testITCTools.generateAcl(null, unitId, true, false, false, true);
		var unit = unitRepository.findById(unitId)
								 .orElseThrow();
		authenticate(userId, Set.of(unit));

		assertTrue(aclAuthorizationService.canRead(aclId));
		assertTrue(aclAuthorizationService.canDelete(aclId));
		assertFalse(aclAuthorizationService.canModify(aclId));
		assertFalse(aclAuthorizationService.canCreate(aclId));
	}

	@Test
	void unitAclDoesNotGrantAccessToNonMembers() {
		var userId = testITCTools.generateUser();
		var unitId = testITCTools.generateUnit();
		var aclId  = testITCTools.generateAcl(null, unitId, true, true, true, true);
		authenticate(userId, Set.of());

		assertFalse(aclAuthorizationService.canRead(aclId));
	}

	@Test
	void aclIdWithoutRowsIsDenied() {
		var userId = testITCTools.generateUser();
		authenticate(userId, Set.of());

		var unusedAclId = aclService.getNextAclId();
		assertFalse(aclAuthorizationService.hasPrivilege(unusedAclId, AclPrivilege.READ,
														 SecurityContextHolder.getContext()
																			  .getAuthentication()));
		assertFalse(aclAuthorizationService.canRead(0L));
	}

	@Test
	void anonymousRequestIsDenied() {
		var userId = testITCTools.generateUser();
		var aclId  = testITCTools.generateAcl(userId, null, true, true, true, true);
		SecurityContextHolder.clearContext();

		assertFalse(aclAuthorizationService.canRead(aclId));
	}

	private void authenticate(Long userId, Set<Unit> units) {
		var account = userAccountRepository.findById(userId)
										   .orElseThrow();
		var principal = new UserDetailsImpl(account.getId(), account.getLoginName(), account.getNick(), account.getEmail(), account.getPassword(),
											units, List.of());
		SecurityContextHolder.getContext()
							 .setAuthentication(new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
	}
}
