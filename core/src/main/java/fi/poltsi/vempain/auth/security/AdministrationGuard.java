package fi.poltsi.vempain.auth.security;

import fi.poltsi.vempain.auth.api.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gate of the user, unit and ACL management endpoints: they are not tied to a content ACL, so they require the modify privilege on the
 * reserved administrator ACL ({@link Constants#ADMIN_ID}), evaluated through the normal ACL rules (never a role).
 */
@Component
@RequiredArgsConstructor
public class AdministrationGuard {
	private final AclAuthorizationService aclAuthorizationService;

	/**
	 * @throws ResponseStatusException 403 when the caller may not administer the service
	 */
	public void requireAdministrator() {
		if (!aclAuthorizationService.canModify(Constants.ADMIN_ID)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required");
		}
	}
}
