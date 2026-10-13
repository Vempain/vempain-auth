package fi.poltsi.vempain.auth.controller;

import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.UnitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The guard clauses of {@link UnitController#findById}: a malformed id never reaches the service and a missing unit is a 404.
 */
@ExtendWith(MockitoExtension.class)
class UnitControllerUTC {
	@Mock
	private UnitService         unitService;
	@Mock
	private AdministrationGuard administrationGuard;

	@InjectMocks
	private UnitController unitController;

	@Test
	void negativeUnitIdIsBadRequest() {
		var exception = assertThrows(ResponseStatusException.class, () -> unitController.findById(-1L));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
		assertEquals("Malformed parameter", exception.getReason());
		verifyNoInteractions(unitService);
	}

	@Test
	void missingUnitIdIsBadRequest() {
		var exception = assertThrows(ResponseStatusException.class, () -> unitController.findById(null));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
		verifyNoInteractions(unitService);
	}

	@Test
	void unknownUnitIsNotFound() throws VempainEntityNotFoundException {
		when(unitService.findById(42L)).thenThrow(new VempainEntityNotFoundException("Unit not found for retrieval", "Unit"));

		var exception = assertThrows(ResponseStatusException.class, () -> unitController.findById(42L));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		assertEquals("No unit was found with given ID", exception.getReason());
	}

	@Test
	void existingUnitIsReturned() throws VempainEntityNotFoundException {
		var unit = UnitResponse.builder()
							   .id(7L)
							   .name("Unit 7")
							   .build();
		when(unitService.findById(7L)).thenReturn(unit);

		var response = unitController.findById(7L);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(unit, response.getBody());
	}
}
