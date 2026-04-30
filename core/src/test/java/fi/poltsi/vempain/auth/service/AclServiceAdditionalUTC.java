package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.exception.VempainAbstractException;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.tools.TestUTCTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AclServiceAdditionalUTC {

	@Mock
	AclRepository         aclRepository;
	@Mock
	UserAccountRepository userAccountRepository;
	@Mock
	UnitRepository        unitRepository;
	@Mock
	AclIdService          aclIdService;

	@InjectMocks
	private AclService aclService;

	@BeforeEach
	void setUp() {
	}

	// ── getAclResponses ───────────────────────────────────────────────────────────

	@Test
	void getAclResponsesOk() {
		List<Acl> acls = TestUTCTools.generateAclList(1L, 3L);
		when(aclRepository.getAclByAclId(1L)).thenReturn(acls);

		var responses = aclService.getAclResponses(1L);
		assertNotNull(responses);
		assertEquals(acls.size(), responses.size());
	}

	@Test
	void getAclResponsesEmptyOk() {
		when(aclRepository.getAclByAclId(99L)).thenReturn(List.of());

		var responses = aclService.getAclResponses(99L);
		assertNotNull(responses);
		assertTrue(responses.isEmpty());
	}

	// ── findAllUserUnitAcls ───────────────────────────────────────────────────────

	@Test
	void findAllUserUnitAclsOk() {
		List<Acl> acls = TestUTCTools.generateAclList(1L, 2L);
		when(aclRepository.findAllWithUserUnit()).thenReturn(acls);

		Iterable<Acl> result = aclService.findAllUserUnitAcls();
		assertNotNull(result);
		long count = StreamSupport.stream(result.spliterator(), false).count();
		assertEquals(acls.size(), count);
	}

	// ── findDuplicateAcls ─────────────────────────────────────────────────────────

	@Test
	void findDuplicateAclsOk() {
		when(aclRepository.findAclIdWithDuplicates()).thenReturn(List.of(1L, 2L));

		List<Long> dupes = aclService.findDuplicateAcls();
		assertNotNull(dupes);
		assertEquals(2, dupes.size());
	}

	@Test
	void findDuplicateAclsEmptyOk() {
		when(aclRepository.findAclIdWithDuplicates()).thenReturn(List.of());

		List<Long> dupes = aclService.findDuplicateAcls();
		assertNotNull(dupes);
		assertTrue(dupes.isEmpty());
	}

	// ── deleteById ────────────────────────────────────────────────────────────────

	@Test
	void deleteByIdOk() {
		doNothing().when(aclRepository).deleteById(1L);

		try {
			aclService.deleteById(1L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}
	}

	// ── createNewAcl ─────────────────────────────────────────────────────────────

	@Test
	void createNewAclOk() {
		when(aclRepository.getNextAclId()).thenReturn(10L);
		var optionalUser = Optional.of(TestUTCTools.generateUser(ADMIN_ID));
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);
		when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

		try {
			Long aclId = aclService.createNewAcl(ADMIN_ID, null, true, true, true, true);
			assertNotNull(aclId);
			assertEquals(10L, aclId);
		} catch (VempainAclException e) {
			fail("Should not throw: " + e.getMessage());
		}
	}

	@Test
	void createNewAclForUnitOk() {
		when(aclRepository.getNextAclId()).thenReturn(10L);
		var unit = Unit.builder().id(1L).name("TestUnit").aclId(1L).build();
		when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
		when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

		try {
			Long aclId = aclService.createNewAcl(null, 1L, true, false, false, false);
			assertNotNull(aclId);
		} catch (VempainAclException e) {
			fail("Should not throw: " + e.getMessage());
		}
	}

	// ── createUniqueAcl ──────────────────────────────────────────────────────────

	@Test
	void createUniqueAclOk() {
		var optionalUser = Optional.of(TestUTCTools.generateUser(ADMIN_ID));
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);

		var expectedAcl = Acl.builder()
							 .id(1L)
							 .aclId(10L)
							 .userId(ADMIN_ID)
							 .readPrivilege(true)
							 .build();
		when(aclIdService.generateNewAcl(ADMIN_ID, null, true, true, true, true)).thenReturn(expectedAcl);

		try {
			Acl result = aclService.createUniqueAcl(ADMIN_ID, null, true, true, true, true);
			assertNotNull(result);
			assertEquals(expectedAcl, result);
		} catch (VempainAclException e) {
			fail("Should not throw: " + e.getMessage());
		}
	}

	@Test
	void createUniqueAclBothNullThrows() {
		assertThrows(VempainAclException.class,
				() -> aclService.createUniqueAcl(null, null, true, true, true, true));
	}

	@Test
	void createUniqueAclInvalidUserThrows() {
		when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(VempainAclException.class,
				() -> aclService.createUniqueAcl(99L, null, true, true, true, true));
	}

	@Test
	void createUniqueAclInvalidUnitThrows() {
		when(unitRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(VempainAclException.class,
				() -> aclService.createUniqueAcl(null, 99L, true, true, true, true));
	}

	@Test
	void createUniqueAclNullReturnedFromServiceThrows() {
		var optionalUser = Optional.of(TestUTCTools.generateUser(ADMIN_ID));
		when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);
		when(aclIdService.generateNewAcl(anyLong(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(null);

		assertThrows(VempainAclException.class,
				() -> aclService.createUniqueAcl(ADMIN_ID, null, true, true, true, true));
	}

	// ── validateAbstractData ─────────────────────────────────────────────────────

	@Test
	void validateAbstractDataValidEntityOk() {
		var unit = buildValidUnit(1L, 1L, ADMIN_ID, null);

		try {
			aclService.validateAbstractData(unit);
		} catch (VempainAbstractException e) {
			fail("Should not throw for a valid entity: " + e.getMessage());
		}
	}

	@Test
	void validateAbstractDataWithModifierAndModifiedOk() {
		var unit = buildValidUnit(1L, 1L, ADMIN_ID, ADMIN_ID);

		try {
			aclService.validateAbstractData(unit);
		} catch (VempainAbstractException e) {
			fail("Should not throw: " + e.getMessage());
		}
	}

	@Test
	void validateAbstractDataInvalidAclIdThrows() {
		var unit = buildValidUnit(1L, 0L, ADMIN_ID, null);

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	@Test
	void validateAbstractDataNullCreatorThrows() {
		var unit = buildValidUnit(1L, 1L, null, null);

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	@Test
	void validateAbstractDataZeroCreatorThrows() {
		var unit = buildValidUnit(1L, 1L, 0L, null);

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	@Test
	void validateAbstractDataNullCreatedThrows() {
		var unit = Unit.builder()
					   .id(1L)
					   .aclId(1L)
					   .name("Unit 1")
					   .creator(ADMIN_ID)
					   .created(null)   // missing
					   .build();

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	@Test
	void validateAbstractDataModifierWithoutModifiedThrows() {
		var unit = Unit.builder()
					   .id(1L)
					   .aclId(1L)
					   .name("Unit 1")
					   .creator(ADMIN_ID)
					   .created(Instant.now().minusSeconds(100))
					   .modifier(ADMIN_ID)
					   .modified(null)   // modifier set but not modified
					   .build();

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	@Test
	void validateAbstractDataModifiedWithoutModifierThrows() {
		var unit = Unit.builder()
					   .id(1L)
					   .aclId(1L)
					   .name("Unit 1")
					   .creator(ADMIN_ID)
					   .created(Instant.now().minusSeconds(100))
					   .modifier(null)          // modifier not set
					   .modified(Instant.now())  // modified set
					   .build();

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	@Test
	void validateAbstractDataModifiedBeforeCreatedThrows() {
		var now = Instant.now();
		var unit = Unit.builder()
					   .id(1L)
					   .aclId(1L)
					   .name("Unit 1")
					   .creator(ADMIN_ID)
					   .created(now)
					   .modifier(ADMIN_ID)
					   .modified(now.minusSeconds(100))  // before created
					   .build();

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	@Test
	void validateAbstractDataInvalidModifierThrows() {
		var now = Instant.now();
		var unit = Unit.builder()
					   .id(1L)
					   .aclId(1L)
					   .name("Unit 1")
					   .creator(ADMIN_ID)
					   .created(now.minusSeconds(100))
					   .modifier(0L)           // invalid modifier
					   .modified(now)
					   .build();

		assertThrows(VempainAbstractException.class, () -> aclService.validateAbstractData(unit));
	}

	// ── helpers ───────────────────────────────────────────────────────────────────

	private Unit buildValidUnit(long id, long aclId, Long creator, Long modifier) {
		var now = Instant.now();
		var builder = Unit.builder()
						  .id(id)
						  .aclId(aclId)
						  .name("Unit " + id)
						  .creator(creator)
						  .created(now.minusSeconds(100));

		if (modifier != null) {
			builder.modifier(modifier).modified(now);
		}

		return builder.build();
	}
}
