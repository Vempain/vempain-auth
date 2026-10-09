package fi.poltsi.vempain.auth.controller;

import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.AclService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclControllerUTC {
	@Mock
	private AclService aclService;
	@Mock
	private AdministrationGuard administrationGuard;

	@InjectMocks
	private AclController aclController;

	@Test
	void missingAclErrorIdentifiesTheRequestedId() {
		when(aclService.findAclByAclId(42L)).thenReturn(List.of());

		var exception = assertThrows(ResponseStatusException.class, () -> aclController.getAcl(42L));

		assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
		assertEquals("No ACL was found for ID 42", exception.getReason());
	}
}
