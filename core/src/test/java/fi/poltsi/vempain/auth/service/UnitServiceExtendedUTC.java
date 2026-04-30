package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.tools.TestUTCTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UnitServiceExtendedUTC {

	@Mock
	private UnitRepository unitRepository;
	@Mock
	private AclRepository  aclRepository;

	@InjectMocks
	private UnitService unitService;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// ── findById – not found ──────────────────────────────────────────────────────

	@Test
	void findByIdNotFoundThrows() {
		when(unitRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(Exception.class, () -> unitService.findById(99L));
	}

	// ── deleteById – unit found ───────────────────────────────────────────────────

	@Test
	void deleteByIdUnitFoundOk() {
		var unit = buildUnit(1L, 5L);
		when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
		doNothing().when(aclRepository).deleteAclsByAclId(5L);
		doNothing().when(aclRepository).deleteAllByUnitId(1L);
		doNothing().when(unitRepository).delete(unit);

		try {
			unitService.deleteById(1L);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}

		verify(aclRepository).deleteAclsByAclId(5L);
		verify(aclRepository).deleteAllByUnitId(1L);
		verify(unitRepository).delete(unit);
	}

	// ── save ──────────────────────────────────────────────────────────────────────

	@Test
	void saveUnitOk() {
		var unit = buildUnit(1L, 5L);
		when(unitRepository.save(unit)).thenReturn(unit);

		var result = unitService.save(unit);
		assertEquals(unit, result);
	}

	// ── createUnit ────────────────────────────────────────────────────────────────

	@Test
	void createUnitOk() {
		setSecurityContext(ADMIN_ID);

		var savedUnit = buildUnit(1L, 1L);
		when(aclRepository.getNextAclId()).thenReturn(1L);
		when(aclRepository.getAclByAclId(anyLong())).thenReturn(new ArrayList<>());
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));
		when(unitRepository.save(any())).thenReturn(savedUnit);

		var req      = buildUnitRequest(1L, ADMIN_ID, null);
		var response = unitService.createUnit(req);
		assertNotNull(response);
	}

	@Test
	void createUnitBadAclThrows() {
		setSecurityContext(ADMIN_ID);

		when(aclRepository.getNextAclId()).thenReturn(1L);

		// empty acl list → VempainAclException → ResponseStatusException
		var req = UnitRequest.builder()
							 .name("TestUnit")
							 .description("A unit")
							 .acls(new ArrayList<>())
							 .build();

		var ex = assertThrows(ResponseStatusException.class, () -> unitService.createUnit(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	// ── updateUnit ────────────────────────────────────────────────────────────────

	@Test
	void updateUnitOk() {
		setSecurityContext(ADMIN_ID);

		var existingUnit = buildUnit(1L, 5L);
		when(unitRepository.findById(1L)).thenReturn(Optional.of(existingUnit));
		when(aclRepository.getAclByAclId(anyLong())).thenReturn(new ArrayList<>());
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));
		when(unitRepository.save(any())).thenReturn(existingUnit);

		var req      = buildUnitRequest(5L, ADMIN_ID, null);
		var response = unitService.updateUnit(1L, req);
		assertNotNull(response);
	}

	@Test
	void updateUnitNotFoundThrows() {
		when(unitRepository.findById(99L)).thenReturn(Optional.empty());

		var req = buildUnitRequest(1L, ADMIN_ID, null);
		var ex  = assertThrows(ResponseStatusException.class, () -> unitService.updateUnit(99L, req));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void updateUnitBadAclThrows() {
		setSecurityContext(ADMIN_ID);

		var existingUnit = buildUnit(1L, 5L);
		when(unitRepository.findById(1L)).thenReturn(Optional.of(existingUnit));

		// empty acl list → VempainAclException → ResponseStatusException
		var req = UnitRequest.builder()
							 .name("UpdatedUnit")
							 .acls(new ArrayList<>())
							 .build();

		var ex = assertThrows(ResponseStatusException.class, () -> unitService.updateUnit(1L, req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	// ── saveAclRequests ───────────────────────────────────────────────────────────

	@Test
	void saveAclRequestsReplacesExistingOk() {
		var existingAcl  = TestUTCTools.generateAcl(1L, 1L, ADMIN_ID, null);
		var aclRequestList = List.of(AclRequest.builder()
											   .aclId(1L)
											   .user(ADMIN_ID)
											   .unit(null)
											   .readPrivilege(true)
											   .createPrivilege(true)
											   .modifyPrivilege(true)
											   .deletePrivilege(true)
											   .build());
		when(aclRepository.getAclByAclId(1L)).thenReturn(List.of(existingAcl));
		doNothing().when(aclRepository).deleteAclsByAclId(1L);
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));

		try {
			unitService.saveAclRequests(1L, aclRequestList);
		} catch (Exception e) {
			fail("Should not throw: " + e.getMessage());
		}
	}

	// ── helpers ───────────────────────────────────────────────────────────────────

	private void setSecurityContext(long userId) {
		var userDetails = new UserDetailsImpl(userId, "login", "nick", "email@test.com",
				"password", Set.of(), null);
		var auth = new UsernamePasswordAuthenticationToken(userDetails, null);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private Unit buildUnit(long id, long aclId) {
		return Unit.builder()
				   .id(id)
				   .aclId(aclId)
				   .name("Unit " + id)
				   .description("Test unit " + id)
				   .build();
	}

	private UnitRequest buildUnitRequest(long aclId, Long userId, Long unitId) {
		return UnitRequest.builder()
						  .name("TestUnit")
						  .description("A unit")
						  .acls(List.of(AclRequest.builder()
												  .aclId(aclId)
												  .user(userId)
												  .unit(unitId)
												  .readPrivilege(true)
												  .createPrivilege(true)
												  .modifyPrivilege(true)
												  .deletePrivilege(true)
												  .build()))
						  .build();
	}
}
