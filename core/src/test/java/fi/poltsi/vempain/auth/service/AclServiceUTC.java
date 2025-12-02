package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.tools.TestUTCTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AclServiceUTC {
	private final static long itemCount = 10L;

	@Mock
	AclRepository         aclRepository;
	@Mock
	UserAccountRepository userAccountRepository;
	@Mock
	UnitRepository        unitRepository;

	@InjectMocks
	private AclService aclService;

	@BeforeEach
	void setUp() {
		reset(aclRepository);
		reset(userAccountRepository);
		reset(unitRepository);
	}

	@Test
	void findAllOk() {
		List<Acl> acls = setupAclList(itemCount);
		when(aclRepository.findAll()).thenReturn(acls);

		Iterable<Acl> aclIterable = aclService.findAll();
		assertNotNull(aclIterable);
		assertEquals((itemCount * 2), StreamSupport.stream(aclIterable.spliterator(), false)
												   .count());
	}

	@Test
	void findAclByAclIdOk() {
		List<Acl> acls = TestUTCTools.generateAclList(1L, 4L);
		when(aclRepository.getAclByAclId(1L)).thenReturn(acls);
		Iterable<Acl> aclIterable = aclService.findAclByAclId(1L);
		assertEquals(8, StreamSupport.stream(aclIterable.spliterator(), false)
									 .count());
	}

	@Test
	void getNextAclIdOk() {
		when(aclRepository.getNextAclId()).thenReturn(11L);
		Long nextId = aclService.getNextAclId();
		assertEquals(11L, nextId);
	}

	@Test
	void deleteByAclIdOk() {
		List<Acl> acls = TestUTCTools.generateAclList(1L, 5L);
		when(aclRepository.getAclByAclId(1L)).thenReturn(acls);
		doNothing().when(aclRepository)
				   .deleteAclsByAclId(1L);

		try {
			aclService.deleteByAclId(1L);
		} catch (Exception e) {
			fail("Deleting existing ACL should have succeeded");
		}
	}

	@Test
	void deleteByAclIdNullFail() {
		when(aclRepository.getAclByAclId(1L)).thenReturn(null);

		try {
			aclService.deleteByAclId(1L);
		} catch (VempainEntityNotFoundException e) {
			assertEquals("ACL not found for deletion", e.getMessage());
			assertEquals("acl", e.getEntityName());
		} catch (Exception e) {
			fail("Deleting existing ACL should have succeeded");
		}
	}

	@Test
	void deleteByAclIdEmptyListFail() {
		when(aclRepository.getAclByAclId(1L)).thenReturn(new ArrayList<>());

		try {
			aclService.deleteByAclId(1L);
		} catch (VempainEntityNotFoundException e) {
			assertEquals("ACL not found for deletion", e.getMessage());
			assertEquals("acl", e.getEntityName());
		} catch (Exception e) {
			fail("Deleting existing ACL should have succeeded");
		}
	}

	@Test
	void saveOk() {
		Acl acl = TestUTCTools.generateAcl(1L, 1L, ADMIN_ID, null);
		when(aclRepository.save(acl)).thenReturn(acl);
		var optionalUser = Optional.of(TestUTCTools.generateUser(ADMIN_ID));
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);

		try {
			aclService.save(acl);
		} catch (Exception e) {
			fail("No exception should have been generated when saving a correct ACL: " + e.getMessage());
		}
	}

	@Test
	void saveWithOnePermissionYesOk() {
		var optionalUser = Optional.of(TestUTCTools.generateUser(ADMIN_ID));
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);

		Integer[] trigger = {1, 2, 3, 4};

		for (Integer i : trigger) {
			Acl acl = Acl.builder()
						 .aclId(1L)
						 .userId(1L)
						 .unitId(null)
						 .readPrivilege((i == 1))
						 .createPrivilege((i == 2))
						 .modifyPrivilege((i == 3))
						 .deletePrivilege((i == 4))
						 .build();

			when(aclRepository.save(acl)).thenReturn(acl);

			try {
				aclService.save(acl);
			} catch (Exception e) {
				fail("No exception should have been generated when saving a correct ACL: " + e.getMessage());
			}
		}
	}

	@Test
	void saveWithAclIdZeroFail() {
		Acl acl = TestUTCTools.generateAcl(1L, 0L, 1L, null);

		try {
			aclService.save(acl);
			fail("No aclId should have caused an exception");
		} catch (VempainAclException e) {
			assertTrue(e.getMessage()
						.contains("Incorrect aclId value"));
		}
	}

	@Test
	void saveWithNegativeAclIdFail() {
		Acl acl = TestUTCTools.generateAcl(1L, -1L, 1L, null);

		try {
			aclService.save(acl);
			fail("No aclId should have caused an exception");
		} catch (VempainAclException e) {
			assertTrue(e.getMessage()
						.contains("Incorrect aclId value"));
		}
	}

	@Test
	void saveWithMissingAclFail() {
		try {
			Acl acl = Acl.builder()
						 .userId(1L)
						 .unitId(null)
						 .readPrivilege(true)
						 .createPrivilege(true)
						 .modifyPrivilege(true)
						 .deletePrivilege(true)
						 .build();

			aclService.save(acl);
			fail("No aclId should have caused an exception");
		} catch (VempainAclException e) {
			assertTrue(e.getMessage()
						.contains("Incorrect aclId value"));
		}
	}

	@Test
	void saveWithNullUserUnitFail() {
		Acl acl = TestUTCTools.generateAcl(1L, 1L, null, null);

		try {
			aclService.save(acl);
			fail("Null user and unit should have caused an exception");
		} catch (VempainAclException e) {
			assertEquals("Both user and unit is null", e.getMessage());
		}
	}

	@Test
	void saveWithUserUnitSetFail() {
		Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, 1L);

		try {
			aclService.save(acl);
			fail("Setting both user and unit to non-null should have caused an exception");
		} catch (VempainAclException e) {
			assertEquals("Both user and unit are set", e.getMessage());
		}
	}

	@Test
	void saveWithAllPermissionsNoFail() {
		var optionalUser = Optional.of(TestUTCTools.generateUser(1L));
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);

		Acl acl = TestUTCTools.generateAcl(1L, 1L, 1L, null);
		acl.setReadPrivilege(false);
		acl.setModifyPrivilege(false);
		acl.setCreatePrivilege(false);
		acl.setDeletePrivilege(false);

		try {
			aclService.save(acl);
			fail("All-NO permission should have caused an exception");
		} catch (VempainAclException e) {
			assertEquals("All permissions set to false, this acl does not make any sense", e.getMessage());
		}
	}

	@Test
	void updateOk() {
		doNothing().when(aclRepository)
				   .update(1L, 1L, null, true, true, true, true);
		Acl acl          = TestUTCTools.generateAcl(1L, 1L, 1L, null);
		var optionalUser = Optional.of(TestUTCTools.generateUser(acl.getUserId()));
		when(userAccountRepository.findById(acl.getUserId())).thenReturn(optionalUser);

		try {
			aclService.update(acl);
		} catch (Exception e) {
			fail("Updating a well-formed ACL should have succeeded: " + e.getMessage());
		}
	}

	@Test
	void updateNoIdFail() {
		Acl acl          = TestUTCTools.generateAcl(null, 1L, ADMIN_ID, null);
		var optionalUser = Optional.of(TestUTCTools.generateUser(ADMIN_ID));
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);

		try {
			aclService.update(acl);
			fail("Updating an ACL with no permission ID should have failed");
		} catch (VempainAclException e) {
			assertEquals("Trying to update ACL with no permission ID", e.getMessage());
		}
	}

	@Test
	void updateFromRequestListOk() {
		List<Acl>        acls     = TestUTCTools.generateAclList(1L, 5L, false);
		List<AclRequest> requests = new ArrayList<>();

		for (Acl acl : acls) {
			var optionalUser = Optional.of(TestUTCTools.generateUser(acl.getUserId()));
			when(userAccountRepository.findById(acl.getUserId())).thenReturn(optionalUser);
			requests.add(AclRequest.builder()
								   .id(acl.getId())
								   .aclId(acl.getAclId())
								   .user(acl.getUserId())
								   .unit(acl.getUnitId())
								   .readPrivilege(acl.isReadPrivilege())
								   .modifyPrivilege(acl.isModifyPrivilege())
								   .createPrivilege(acl.isCreatePrivilege())
								   .deletePrivilege(acl.isDeletePrivilege())
								   .build());
			when(aclRepository.getAclByAclId(acl.getAclId())).thenReturn(acls.stream()
																			 .filter(a -> a.getAclId() == acl.getAclId())
																			 .collect(Collectors.toList()));
		}

		doNothing().when(aclRepository)
				   .deleteAclsByAclId(anyLong());
		when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

		try {
			aclService.updateFromRequestList(requests);
		} catch (VempainAclException e) {
			fail("Updating with a valid AclRequest-array should have succeeded: " + e.getMessage());
		}
	}

	@Test
	void updateFromRequestListNoPriorAclsOk() {
		List<Acl>        acls     = TestUTCTools.generateAclList(1L, 5L, false);
		List<AclRequest> requests = new ArrayList<>();

		for (Acl acl : acls) {
			requests.add(AclRequest.builder()
								   .id(acl.getId())
								   .aclId(acl.getAclId())
								   .user(acl.getUserId())
								   .unit(acl.getUnitId())
								   .readPrivilege(acl.isReadPrivilege())
								   .modifyPrivilege(acl.isModifyPrivilege())
								   .createPrivilege(acl.isCreatePrivilege())
								   .deletePrivilege(acl.isDeletePrivilege())
								   .build());
			when(aclRepository.getAclByAclId(acl.getAclId())).thenReturn(new ArrayList<>());
			var optionalUser = Optional.of(TestUTCTools.generateUser(1L));
			when(userAccountRepository.findById(acl.getUserId())).thenReturn(optionalUser);
		}

		when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

		try {
			aclService.updateFromRequestList(requests);
		} catch (VempainAclException e) {
			fail("Updating with a valid AclRequest-array should have succeeded: " + e.getMessage());
		}
	}

	@Test
	void updateFromRequestListNullListIdFail() {
		try {
			aclService.updateFromRequestList(null);
			fail("Sending null request list should have failed");
		} catch (VempainAclException e) {
			assertEquals("No ACL entries in request", e.getMessage());
		}
	}

	@Test
	void updateFromRequestListEmptyListIdFail() {
		try {
			aclService.updateFromRequestList(new ArrayList<>());
			fail("Sending empty request list should have failed");
		} catch (VempainAclException e) {
			assertEquals("No ACL entries in request", e.getMessage());
		}
	}

	@Test
	void updateFromRequestListMultipleAclIdFail() {
		List<Acl>        acls     = setupAclList(2L);
		List<AclRequest> requests = new ArrayList<>();

		for (Acl acl : acls) {
			requests.add(AclRequest.builder()
								   .id(acl.getId())
								   .aclId(acl.getAclId())
								   .user(acl.getUserId())
								   .unit(acl.getUnitId())
								   .readPrivilege(acl.isReadPrivilege())
								   .modifyPrivilege(acl.isModifyPrivilege())
								   .createPrivilege(acl.isCreatePrivilege())
								   .deletePrivilege(acl.isDeletePrivilege())
								   .build());
		}

		try {
			aclService.updateFromRequestList(requests);
			fail("Sending multiple ACL IDs in the request array should have failed");
		} catch (VempainAclException e) {
			assertEquals("List of ACL request does not have all the same aclId", e.getMessage());
		}
	}

	@Test
	void saveAclRequestsOk() {
		List<Acl>        acls     = TestUTCTools.generateAclList(1L, 4L, false);
		List<AclRequest> requests = new ArrayList<>();

		for (Acl acl : acls) {
			var optionalUser = Optional.of(TestUTCTools.generateUser(acl.getUserId()));
			when(userAccountRepository.findById(acl.getUserId())).thenReturn(optionalUser);

			requests.add(AclRequest.builder()
								   .id(null)
								   .aclId(acl.getAclId())
								   .user(acl.getUserId())
								   .unit(acl.getUnitId())
								   .readPrivilege(acl.isReadPrivilege())
								   .modifyPrivilege(acl.isModifyPrivilege())
								   .createPrivilege(acl.isCreatePrivilege())
								   .deletePrivilege(acl.isDeletePrivilege())
								   .build());
			when(aclRepository.getAclByAclId(acl.getAclId())).thenReturn(new ArrayList<>());
		}

		when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

		try {
			aclService.saveAclRequests(1L, requests);
		} catch (VempainAclException e) {
			fail("Saving a valid AclRequest-array should have succeeded: " + e.getMessage());
		}
	}

	@Test
	void saveAclRequestsNegativeAclIdFail() {
		try {
			aclService.saveAclRequests(-1L, new ArrayList<>());
			fail("Saving a negative ACL ID should have failed");
		} catch (VempainAclException e) {
			assertEquals("New ACL ID is invalid", e.getMessage());
		}
	}

	@Test
	void saveAclRequestsNullAclIdFail() {
		try {
			aclService.saveAclRequests(null, new ArrayList<>());
			fail("Saving a null ACL ID should have failed");
		} catch (VempainAclException e) {
			assertEquals("New ACL ID is invalid", e.getMessage());
		}
	}

	@Test
	void saveAclRequestsNullRequestFail() {
		try {
			aclService.saveAclRequests(1L, null);
			fail("Saving a null AclRequest-array should have failed");
		} catch (VempainAclException e) {
			assertEquals("No ACL to save", e.getMessage());
		}
	}

	@Test
	void saveAclRequestsEmptyRequestFail() {
		try {
			aclService.saveAclRequests(1L, new ArrayList<>());
			fail("Saving an empty AclRequest-array should have failed");
		} catch (VempainAclException e) {
			assertEquals("No ACL to save", e.getMessage());
		}
	}

	@Test
	void saveNewAclForObjectOk() {
		List<Acl>        acls        = TestUTCTools.generateAclList(1L, 10L, false);
		List<AclRequest> aclRequests = TestUTCTools.generateAclRequestListFromAcl(acls);
		when(aclRepository.getNextAclId()).thenReturn(1L);
		when(aclRepository.getAclByAclId(1L)).thenReturn(new ArrayList<>());

		for (Acl acl : acls) {
			var optionalUser = Optional.of(TestUTCTools.generateUser(acl.getUserId()));
			when(userAccountRepository.findById(acl.getUserId())).thenReturn(optionalUser);
			when(aclRepository.save(acl)).thenReturn(acl);
		}

		try {
			aclService.saveNewAclForObject(aclRequests);
		} catch (Exception e) {
			fail("Saving new ACL from ACL request list should have succeeded");
		}
	}

	@Test
	void saveNewAclForObjectNegativeAclIdFail() {
		ArrayList<AclRequest> aclRequests = new ArrayList<>();
		when(aclRepository.getNextAclId()).thenReturn(-1L);

		try {
			aclService.saveNewAclForObject(aclRequests);
			fail("Saving an empty ACL request list should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"Invalid ACL ID\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have caught any other exception: " + e);
		}
	}

	@Test
	void saveNewAclForObjectNullRequestFail() {
		when(aclRepository.getNextAclId()).thenReturn(1L);

		try {
			aclService.saveNewAclForObject(null);
			fail("Saving a null AclRequest-array should have failed");
		} catch (ResponseStatusException e) {
			assertEquals("400 BAD_REQUEST \"Request contains no ACL list\"", e.getMessage());
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have caught any other exception: " + e);
		}
	}

	@Test
	void saveNewAclForObjectRuntimeExceptionFail() {
		List<Acl>        acls        = TestUTCTools.generateAclList(1L, 1L, false);
		List<AclRequest> aclRequests = TestUTCTools.generateAclRequestListFromAcl(acls);
		when(aclRepository.getNextAclId()).thenReturn(1L);

		for (var acl : acls) {
			var optionalUser = Optional.of(TestUTCTools.generateUser(acl.getUserId()));
			when(userAccountRepository.findById(acl.getUserId())).thenReturn(optionalUser);
		}

		doThrow(new RuntimeException("Test exception")).when(aclRepository)
													   .save(any());

		try {
			aclService.saveNewAclForObject(aclRequests);
			fail("We should have received an exception when repository save throws a runtime exception");
		} catch (ResponseStatusException e) {
			assertEquals("500 INTERNAL_SERVER_ERROR \"Unknown error\"", e.getMessage());
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
		} catch (Exception e) {
			fail("We should not have caught any other exception: " + e);
		}
	}


	private List<Acl> setupAclList(long counter) {
		ArrayList<Acl> acls   = new ArrayList<>();
		long           aclIdx = 1L;
		long           idx    = 1L;

		for (long i = 0; i < counter; i++) {
			acls.add(Acl.builder()
						.id(idx)
						.aclId(aclIdx)
						.userId(1L)
						.unitId(null)
						.readPrivilege(true)
						.createPrivilege(true)
						.modifyPrivilege(true)
						.deletePrivilege(true)
						.build());
			idx++;
			acls.add(Acl.builder()
						.id(idx)
						.aclId(aclIdx)
						.userId(1L)
						.unitId(null)
						.readPrivilege(true)
						.createPrivilege(true)
						.modifyPrivilege(true)
						.deletePrivilege(true)
						.build());
			idx++;
			aclIdx++;
		}
		return acls;
	}
}
