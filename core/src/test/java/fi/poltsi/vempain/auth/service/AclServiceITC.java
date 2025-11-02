package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Instant;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest(classes = AclServiceITC.TestApp.class)
class AclServiceITC extends IntegrationTestSetup {
	static final long initCount = 5;

	@Autowired
	private AclService     aclService;
	@Autowired
	private AclRepository  aclRepository;
	@Autowired
	private UnitRepository unitRepository;

	@Test
	void getAllOk() {
		testITCTools.generateAcls(initCount);
		var acls = aclService.findAll();
		assertTrue(StreamSupport.stream(acls.spliterator(), false)
								.count() >= 7 * initCount);
	}

	@Test
	void getAclByAclIdOk() {
		var aclIds = testITCTools.generateAcls(initCount);
		assertNotNull(aclIds);
		assertFalse(aclIds.isEmpty());
		assertEquals(initCount, aclIds.size());
		var acls = aclService.findAclByAclId(aclIds.getFirst());
		assertNotNull(acls);
		assertEquals(2L, acls.size());

		var userCount = 0L;
		var unitCount = 0L;

		for (Acl acl : acls) {
			if (acl.getUnitId() != null) {
				unitCount++;
				assertNull(acl.getUserId());
			}

			if (acl.getUserId() != null) {
				userCount++;
			}

		}

		assertEquals(1, userCount);
		assertEquals(1, unitCount);
	}

	@Test
	void deleteByAclIdOk() {
		var aclIds = testITCTools.generateAcls(initCount);

		try {
			for (Long aclId : aclIds) {
				aclService.deleteByAclId(aclId);
			}
		} catch (VempainEntityNotFoundException e) {
			fail("Should have been able to delete Acl with id: " + (initCount - 1));
		}
	}

	@Test
	void saveOk() {
		var userId  = testITCTools.generateUser();
		var nextAcl = aclService.getNextAclId();
		Acl acl = Acl.builder()
					 .aclId(nextAcl)
					 .userId(userId)
					 .unitId(null)
					 .readPrivilege(true)
					 .createPrivilege(true)
					 .modifyPrivilege(true)
					 .deletePrivilege(true)
					 .build();

		try {
			aclService.save(acl);
		} catch (Exception e) {
			fail("Creating ACL should have succeeded: " + e);
		}
	}

	@Test
	void updateOk() {
		var aclList = testITCTools.generateAcls(initCount);
		assertFalse(aclList.isEmpty());
		var acls = aclService.findAclByAclId(aclList.getFirst());
		assertNotNull(acls);
		assertEquals(2L, acls.size());

		try {
			for (Acl acl : acls) {
				acl.setReadPrivilege(false);
				aclService.update(acl);
			}
		} catch (VempainAclException e) {
			fail("Updating ACL should have succeeded");
		}

		var checkAcls = aclService.findAclByAclId(aclList.getFirst());
		assertNotNull(checkAcls);
		assertEquals(2L, checkAcls.size());

		for (Acl acl : checkAcls) {
			assertFalse(acl.isReadPrivilege());
			assertTrue(acl.isCreatePrivilege());
			assertTrue(acl.isModifyPrivilege());
			assertTrue(acl.isDeletePrivilege());
		}
	}

	@Test
	void updateFailsWithZeroAclId() {
		var           aclIds = testITCTools.generateAcls(initCount);
		Iterable<Acl> acls   = aclService.findAclByAclId(aclIds.getFirst());

		for (Acl acl : acls) {
			// The aclId is set to zero
			try {
				acl.setAclId(0L);
				aclService.update(acl);
				fail("AclId 0 should have caused an exception");
			} catch (VempainAclException e) {
				assertTrue(e.getMessage()
							.contains("Incorrect aclId value"));
			}
		}
	}

	@Test
	void updateFailsWithNegativeAclId() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(initCount - 1);

		for (Acl acl : acls) {
			// The aclId is set to zero
			try {
				acl.setAclId(-1L);
				aclService.update(acl);
				fail("AclId 0 should have caused an exception");
			} catch (VempainAclException e) {
				assertTrue(e.getMessage()
							.contains("Incorrect aclId value"));
			}
		}
	}

	@Test
	void updateFailsWithNullUserAndUnit() {
		var           aclIdList = testITCTools.generateAcls(initCount);
		Iterable<Acl> acls      = aclService.findAclByAclId(aclIdList.getFirst());

		for (Acl acl : acls) {
			// Update with both null user and unit
			try {
				acl.setUserId(null);
				acl.setUnitId(null);
				aclService.update(acl);
				fail("Null user and unit should have caused an exception");
			} catch (VempainAclException e) {
				assertTrue(e.getMessage()
							.contains("Both user and unit is null"));
			}
		}
	}

	@Test
	void updateFailsWithUserAndUnitSet() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(initCount - 1);

		for (Acl acl : acls) {
			// Update with both user and unit set
			try {
				acl.setUserId(1L);
				acl.setUnitId(1L);
				aclService.update(acl);
				fail("Set user and unit should have caused an exception");
			} catch (VempainAclException e) {
				assertTrue(e.getMessage()
							.contains("Both user and unit are set"));
			}
		}
	}

	@Test
	void updateWithAllPermissionsNoFail() {
		testITCTools.generateAcls(initCount);
		Iterable<Acl> acls = aclService.findAclByAclId(initCount - 1);

		for (Acl acl : acls) {
			// All permissions set to no does not make sense
			try {
				acl.setReadPrivilege(false);
				acl.setCreatePrivilege(false);
				acl.setModifyPrivilege(false);
				acl.setDeletePrivilege(false);
				aclService.update(acl);
				fail("All-NO permission should have caused an exception");
			} catch (VempainAclException e) {
				log.info("Exception message: {}", e.getMessage());
				assertTrue(e.getMessage()
							.contains("All permissions set to false, this acl does not make any sense"));
			}
		}
	}

	@Test
	void createFindDeleteAclsOk() throws Exception {
		var unit = Unit.builder()
					   .name("ACL_UNIT")
					   // add required audit fields
					   .creator(1L)
					   .created(Instant.now())
					   // if Unit extends AbstractVempainEntity, aclId must be positive
					   .aclId(1L)
					   .build();
		unit = unitRepository.save(unit);

		var aclId = aclService.createNewAcl(
				null,               // userId
				unit.getId(),       // unitId
				true,               // readPrivilege
				false,              // createPrivilege
				false,              // modifyPrivilege
				false               // deletePrivilege
		);

		var stored = aclService.findAclByAclId(aclId);
		assertFalse(stored.isEmpty());
		assertEquals(stored.getFirst()
						   .getUnitId(), unit.getId());

		aclService.deleteByAclId(aclId);
		assertTrue(aclRepository.getAclByAclId(aclId)
								.isEmpty());
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EnableJpaRepositories(basePackages = "fi.poltsi.vempain.auth.repository")
	@EntityScan(basePackages = "fi.poltsi.vempain.auth.entity")
	@ComponentScan(basePackages = {
			"fi.poltsi.vempain.auth.service",
			"fi.poltsi.vempain.auth.repository",
			"fi.poltsi.vempain.auth.tools" // added so TestITCTools is discovered
	})
	static class TestApp {
	}
}
