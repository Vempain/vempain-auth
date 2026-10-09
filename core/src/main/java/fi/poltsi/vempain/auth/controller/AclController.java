package fi.poltsi.vempain.auth.controller;

import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.rest.AclAPI;
import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.AclService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Read-only ACL listing hosted by every service that includes {@code vempain-auth-core}.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class AclController implements AclAPI {
	private final AclService          aclService;
	private final AdministrationGuard administrationGuard;

	@Override
	public ResponseEntity<List<AclResponse>> getAllAcl() {
		administrationGuard.requireAdministrator();
		return ResponseEntity.ok(aclService.findAll()
										   .stream()
										   .map(Acl::toResponse)
										   .toList());
	}

	@Override
	public ResponseEntity<List<AclResponse>> getAcl(Long aclId) {
		administrationGuard.requireAdministrator();
		var rows = aclService.findAclByAclId(aclId);

		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No ACL was found for ID " + aclId);
		}

		return ResponseEntity.ok(rows.stream()
									 .map(Acl::toResponse)
									 .toList());
	}
}
