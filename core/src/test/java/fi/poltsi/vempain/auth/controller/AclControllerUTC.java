package fi.poltsi.vempain.auth.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.auth.api.AclBulkUpdateModeEnum;
import fi.poltsi.vempain.auth.api.request.AclBulkEntryRequest;
import fi.poltsi.vempain.auth.api.request.AclBulkUpdateRequest;
import fi.poltsi.vempain.auth.api.response.AclBulkUpdateResponse;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.security.AclAuthorizationService;
import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.AclService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclControllerUTC {

    @Mock
    private AclService aclService;
    @Mock
    private AdministrationGuard administrationGuard;

    @Mock
    private AclAuthorizationService aclAuthorizationService;
    @InjectMocks
    private AclController aclController;

    @Test
    void bulkUpdateDelegatesWithTheCallersModifyCheckAndMapsRejectionsTo400() throws Exception {
        var request = AclBulkUpdateRequest.builder()
            .aclIds(List.of(41L, 42L))
            .mode(AclBulkUpdateModeEnum.ADD)
            .entries(List.of(AclBulkEntryRequest.builder()
                .user(7L)
                .readPrivilege(true)
                .build()))
            .build();
        var response = AclBulkUpdateResponse.builder()
            .updatedAclIds(List.of(41L))
            .skippedAclIds(List.of(42L))
            .build();
        when(aclService.bulkUpdate(eq(request.getAclIds()), eq(AclBulkUpdateModeEnum.ADD), eq(request.getEntries()), any())).thenReturn(response);

        assertEquals(response, aclController.bulkUpdate(request)
            .getBody());
        // the administrator guard is not involved: the check is the modify privilege on every ACL
        verify(administrationGuard, never()).requireAdministrator();

        when(aclService.bulkUpdate(any(), any(), any(), any())).thenThrow(new VempainAclException("Invalid user ID given in the ACL"));
        var exception = assertThrows(ResponseStatusException.class, () -> aclController.bulkUpdate(request));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid user ID given in the ACL", exception.getReason());
    }

    @Test
    void missingAclErrorIdentifiesTheRequestedId() {
        when(aclService.findAclByAclId(42L)).thenReturn(List.of());

        var exception = assertThrows(ResponseStatusException.class, () -> aclController.getAcl(42L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("No ACL was found for ID 42", exception.getReason());
    }
}
