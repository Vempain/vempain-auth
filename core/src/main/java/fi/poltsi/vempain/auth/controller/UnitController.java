package fi.poltsi.vempain.auth.controller;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.exception.VempainEntityNotFoundException;
import fi.poltsi.vempain.auth.rest.UnitAPI;
import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Unit management hosted by every service that includes {@code vempain-auth-core}. Member changes go through
 * {@code UnitService.updateMembers}, which rejects circular unit nesting.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class UnitController implements UnitAPI {
	private final UnitService         unitService;
	private final AdministrationGuard administrationGuard;

	@Override
	public ResponseEntity<List<UnitResponse>> getUnits() {
		return ResponseEntity.ok(unitService.findAllResponses());
	}

	@Override
	public ResponseEntity<PagedResponse<UnitResponse>> getPagedUnits(PagedRequest request) {
		return ResponseEntity.ok(unitService.findPaged(request));
	}

	@Override
	public ResponseEntity<UnitResponse> findById(Long unitId) {
		if (unitId == null || unitId < 0) {
			log.error("Invalid unit ID: {}", unitId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed parameter");
		}

		try {
			return ResponseEntity.ok(unitService.findById(unitId));
		} catch (VempainEntityNotFoundException e) {
			log.error("Could not find any unit by id {}", unitId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No unit was found with given ID");
		}
	}

	@Override
	public ResponseEntity<UnitResponse> addUnit(UnitRequest unitRequest) {
		administrationGuard.requireAdministrator();
		return ResponseEntity.ok(unitService.createUnit(unitRequest));
	}

	@Override
	public ResponseEntity<UnitResponse> updateUnit(Long unitId, UnitRequest unitRequest) {
		administrationGuard.requireAdministrator();
		return ResponseEntity.ok(unitService.updateUnit(unitId, unitRequest));
	}
}
