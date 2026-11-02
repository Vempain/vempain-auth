package fi.poltsi.vempain.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.auth.api.request.AclBulkUpdateRequest;
import fi.poltsi.vempain.auth.api.response.AclBulkUpdateResponse;
import fi.poltsi.vempain.auth.api.response.AclResponse;
import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.rest.AclAPI;
import fi.poltsi.vempain.auth.security.AclAuthorizationService;
import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.AclService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The ACL endpoints hosted by every service that includes {@code vempain-auth-core}: the administrator-only listing and the bulk update of the ACLs of selected
 * resources, which changes only the ACLs granting the caller the modify privilege.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class AclController implements AclAPI {

    private final AclService aclService;
    private final AdministrationGuard administrationGuard;
    private final AclAuthorizationService aclAuthorizationService;

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

    @Override
    public ResponseEntity<AclBulkUpdateResponse> bulkUpdate(AclBulkUpdateRequest request) {
        try {
            return ResponseEntity.ok(aclService.bulkUpdate(request.getAclIds(), request.getMode(), request.getEntries(), aclAuthorizationService::canModify));
        } catch (VempainAclException e) {
            log.warn("Bulk ACL update rejected: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
