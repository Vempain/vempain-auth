package fi.poltsi.vempain.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.exception.VempainAbstractException;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.tools.TestUTCTools;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AclServiceAdditionalUTC {

    @Mock
    AclRepository aclRepository;
    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    UnitRepository unitRepository;
    @Mock
    AclIdService aclIdService;

    @InjectMocks
    private AclService aclService;

    @BeforeEach
    void setUp() {}

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
        long count = StreamSupport.stream(result.spliterator(), false)
            .count();
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
        doNothing().when(aclRepository)
            .deleteById(1L);

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
        var unit = Unit.builder()
            .id(1L)
            .name("TestUnit")
            .aclId(1L)
            .build();
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
        assertThrows(VempainAclException.class, () -> aclService.createUniqueAcl(null, null, true, true, true, true));
    }

    @Test
    void createUniqueAclInvalidUserThrows() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VempainAclException.class, () -> aclService.createUniqueAcl(99L, null, true, true, true, true));
    }

    @Test
    void createUniqueAclInvalidUnitThrows() {
        when(unitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VempainAclException.class, () -> aclService.createUniqueAcl(null, 99L, true, true, true, true));
    }

    @Test
    void createUniqueAclNullReturnedFromServiceThrows() {
        var optionalUser = Optional.of(TestUTCTools.generateUser(ADMIN_ID));
        when(userAccountRepository.findById(ADMIN_ID)).thenReturn(optionalUser);
        when(aclIdService.generateNewAcl(anyLong(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(null);

        assertThrows(VempainAclException.class, () -> aclService.createUniqueAcl(ADMIN_ID, null, true, true, true, true));
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
            .created(Instant.now()
                .minusSeconds(100))
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
            .created(Instant.now()
                .minusSeconds(100))
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
            builder.modifier(modifier)
                .modified(now);
        }

        return builder.build();
    }

    // ── updateFromRequestList ────────────────────────────────────────────────────

    @Test
    void updateFromRequestListAllocatesAnAclIdForNewRequests() throws VempainAclException {
        var requests = List.of(aclRequest(0L, ADMIN_ID, null), aclRequest(0L, null, 7L));
        when(aclRepository.getNextAclId()).thenReturn(50L);
        when(aclRepository.getAclByAclId(50L)).thenReturn(List.of());
        when(userAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(TestUTCTools.generateUser(ADMIN_ID)));
        when(unitRepository.findById(7L)).thenReturn(Optional.of(TestUTCTools.generateUnit(7L)));
        when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

        aclService.updateFromRequestList(requests);

        assertTrue(requests.stream()
            .allMatch(request -> request.getAclId() == 50L));
        verify(aclRepository, never()).deleteAclsByAclId(anyLong());
        verify(aclRepository, times(2)).save(any(Acl.class));
    }

    // ── saveAclRequests ──────────────────────────────────────────────────────────

    @Test
    void saveAclRequestsReplacesExistingRows() throws VempainAclException {
        var existing = TestUTCTools.generateAcl(1L, 5L, ADMIN_ID, null);
        when(aclRepository.getAclByAclId(5L)).thenReturn(List.of(existing));
        when(userAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(TestUTCTools.generateUser(ADMIN_ID)));
        when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

        aclService.saveAclRequests(5L, List.of(aclRequest(5L, ADMIN_ID, null)));

        verify(aclRepository).deleteAclsByAclId(5L);
        verify(aclRepository).save(any(Acl.class));
    }

    @Test
    void saveAclRequestsToleratesRowsVanishingBeforeDeletion() throws VempainAclException {
        var existing = TestUTCTools.generateAcl(1L, 5L, ADMIN_ID, null);
        // The rows exist when checked but are gone when the deletion looks them up again
        when(aclRepository.getAclByAclId(5L)).thenReturn(List.of(existing))
            .thenReturn(List.of());
        when(userAccountRepository.findById(ADMIN_ID)).thenReturn(Optional.of(TestUTCTools.generateUser(ADMIN_ID)));
        when(aclRepository.save(any(Acl.class))).thenAnswer(a -> a.getArgument(0));

        aclService.saveAclRequests(5L, List.of(aclRequest(5L, ADMIN_ID, null)));

        verify(aclRepository, never()).deleteAclsByAclId(anyLong());
        verify(aclRepository).save(any(Acl.class));
    }

    // ── save: the user or unit must exist ────────────────────────────────────────

    @Test
    void saveWithUnknownUserFails() {
        when(userAccountRepository.findById(Long.valueOf(99L))).thenReturn(Optional.empty());
        var acl = TestUTCTools.generateAcl(null, 5L, 99L, null);

        var exception = assertThrows(VempainAclException.class, () -> aclService.save(acl));

        assertEquals("Invalid user ID given in the ACL", exception.getMessage());
        verify(aclRepository, never()).save(any(Acl.class));
    }

    @Test
    void saveWithUnknownUnitFails() {
        when(unitRepository.findById(99L)).thenReturn(Optional.empty());
        var acl = TestUTCTools.generateAcl(null, 5L, null, 99L);

        var exception = assertThrows(VempainAclException.class, () -> aclService.save(acl));

        assertEquals("Invalid unit ID given in the ACL", exception.getMessage());
        verify(aclRepository, never()).save(any(Acl.class));
    }

    // ── saveNewAclForObject ──────────────────────────────────────────────────────

    @Test
    void saveNewAclForObjectMapsAnAlreadyExistingAclIdToBadRequest() {
        when(aclRepository.getNextAclId()).thenReturn(5L);
        when(aclRepository.getAclByAclId(5L)).thenThrow(new IllegalStateException("Trying to save an ACL ID that already exists"));
        var requests = List.of(aclRequest(0L, ADMIN_ID, null));

        var exception = assertThrows(ResponseStatusException.class, () -> aclService.saveNewAclForObject(requests));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Request contains already existing ACL ID", exception.getReason());
    }

    private static AclRequest aclRequest(long aclId, Long userId, Long unitId) {
        return AclRequest.builder()
            .aclId(aclId)
            .user(userId)
            .unit(unitId)
            .readPrivilege(true)
            .createPrivilege(true)
            .modifyPrivilege(true)
            .deletePrivilege(true)
            .build();
    }
}
