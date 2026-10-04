package fi.poltsi.vempain.auth.security;

import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Evaluates resource permissions from ACL rows for the authenticated user.
 *
 * <p>ACL entries may target either a user or one of the user's units. An
 * authenticated user must have at least one matching entry with the requested
 * privilege. There is deliberately no role or authority fallback.</p>
 */
@Service
@RequiredArgsConstructor
public class AclAuthorizationService {
	private final AclRepository aclRepository;

	public boolean canRead(long aclId) {
		return hasPrivilege(aclId, AclPrivilege.READ, currentAuthentication());
	}

	public boolean canCreate(long aclId) {
		return hasPrivilege(aclId, AclPrivilege.CREATE, currentAuthentication());
	}

	public boolean canModify(long aclId) {
		return hasPrivilege(aclId, AclPrivilege.MODIFY, currentAuthentication());
	}

	public boolean canDelete(long aclId) {
		return hasPrivilege(aclId, AclPrivilege.DELETE, currentAuthentication());
	}

	public boolean hasPrivilege(long aclId, AclPrivilege privilege, Authentication authentication) {
		if (aclId < 1 || authentication == null || !authentication.isAuthenticated()) {
			return false;
		}

		if (!(authentication.getPrincipal() instanceof UserDetailsImpl user)) {
			return false;
		}

		var userId = user.getId();
		var unitIds = user.getUnits() == null
					  ? java.util.Set.<Long>of()
					  : user.getUnits()
							.stream()
							.map(Unit::getId)
							.filter(Objects::nonNull)
							.collect(java.util.stream.Collectors.toSet());

		return aclRepository.getAclByAclId(aclId)
							.stream()
							.filter(acl -> Objects.equals(acl.getUserId(), userId)
										   || (acl.getUserId() == null && acl.getUnitId() != null && unitIds.contains(acl.getUnitId())))
							.anyMatch(acl -> privilege.isGranted(acl));
	}

	private Authentication currentAuthentication() {
		return SecurityContextHolder.getContext()
									.getAuthentication();
	}

	public enum AclPrivilege {
		READ {
			@Override
			boolean isGranted(Acl acl) {
				return acl.isReadPrivilege();
			}
		},
		CREATE {
			@Override
			boolean isGranted(Acl acl) {
				return acl.isCreatePrivilege();
			}
		},
		MODIFY {
			@Override
			boolean isGranted(Acl acl) {
				return acl.isModifyPrivilege();
			}
		},
		DELETE {
			@Override
			boolean isGranted(Acl acl) {
				return acl.isDeletePrivilege();
			}
		};

		abstract boolean isGranted(Acl acl);
	}
}
