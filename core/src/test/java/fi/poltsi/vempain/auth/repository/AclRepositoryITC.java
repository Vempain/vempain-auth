package fi.poltsi.vempain.auth.repository;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.entity.Acl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest(classes = TestApp.class)
class AclRepositoryITC extends IntegrationTestSetup {
	private final static long initCount = 10L;

	@Test
	void getNextAclId() {
		testITCTools.generateAcls(initCount);
		Long nextId = aclRepository.getNextAvailableAclId();
		assertNotNull(nextId);
		log.info("Next ID: {}", nextId);

		assertTrue(nextId > 0);
		aclRepository.deleteAll();

		nextId = aclRepository.getNextAvailableAclId();
		assertNotNull(nextId);
		log.info("Next ID: {}", nextId);

		assertEquals(1L, nextId);
	}

	@Test
	void getAllAclOk() {
		testITCTools.generateAcls(initCount);
		var aclList = aclRepository.findAll();
		assertNotNull(aclList);
		assertTrue(aclList.size() >= initCount);
	}

	@Test
	@Transactional
	void deleteAcl() {
		var userId = testITCTools.generateUser();
		var aclId  = testITCTools.generateAcl(userId, null, true, true, true, true);

		List<Acl> aclList = aclRepository.getAclByAclId(aclId);
		assertNotNull(aclList);
		assertEquals(1, aclList.size());
		assertEquals(aclId, aclList.getFirst()
								   .getAclId());
		log.info("Found acl from database with ID: {}", aclList.getFirst()
															   .getAclId());
		aclRepository.deleteAclsByAclId(aclId);
		List<Acl> emptyAclList = aclRepository.getAclByAclId(aclId);
		assertNotNull(emptyAclList);
		assertTrue(emptyAclList.isEmpty());
	}
}
