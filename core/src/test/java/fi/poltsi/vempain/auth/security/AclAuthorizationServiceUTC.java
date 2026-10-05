package fi.poltsi.vempain.auth.security;

import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.security.AclAuthorizationService.AclPrivilege;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AclAuthorizationServiceUTC {
	private static final long ACL_ID  = 42L;
	private static final long USER_ID = 7L;
	private static final long UNIT_ID = 11L;

	@Mock
	private AclRepository aclRepository;

	@InjectMocks
	private AclAuthorizationService aclAuthorizationService;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void canReadGrantedByUserAcl() {
		authenticate(user(USER_ID, Set.of()));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(USER_ID, null, true, false, false, false)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isTrue();
		assertThat(aclAuthorizationService.canCreate(ACL_ID)).isFalse();
		assertThat(aclAuthorizationService.canModify(ACL_ID)).isFalse();
		assertThat(aclAuthorizationService.canDelete(ACL_ID)).isFalse();
	}

	@Test
	void canCreateModifyDeleteEvaluateTheirOwnPrivilege() {
		authenticate(user(USER_ID, Set.of()));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(USER_ID, null, false, true, true, true)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
		assertThat(aclAuthorizationService.canCreate(ACL_ID)).isTrue();
		assertThat(aclAuthorizationService.canModify(ACL_ID)).isTrue();
		assertThat(aclAuthorizationService.canDelete(ACL_ID)).isTrue();
	}

	@Test
	void unitAclGrantsWhenUserBelongsToUnit() {
		authenticate(user(USER_ID, Set.of(unit(UNIT_ID))));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(null, UNIT_ID, true, false, false, false)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isTrue();
	}

	@Test
	void unitAclIsIgnoredWhenAclAlsoNamesAnotherUser() {
		authenticate(user(USER_ID, Set.of(unit(UNIT_ID))));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(USER_ID + 1, UNIT_ID, true, false, false, false)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
	}

	@Test
	void unitAclForUnrelatedUnitIsDenied() {
		authenticate(user(USER_ID, Set.of(unit(UNIT_ID))));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(null, UNIT_ID + 1, true, true, true, true)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
	}

	@Test
	void unitsWithoutIdAreIgnored() {
		var units = new HashSet<Unit>();
		units.add(Unit.builder()
		              .build());
		units.add(unit(UNIT_ID));
		authenticate(user(USER_ID, units));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(null, UNIT_ID, true, false, false, false)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isTrue();
	}

	@Test
	void nullUnitSetIsTreatedAsNoUnits() {
		authenticate(user(USER_ID, null));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(null, UNIT_ID, true, true, true, true)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
	}

	@Test
	void aclRowWithoutUserAndUnitGrantsNothing() {
		authenticate(user(USER_ID, Set.of(unit(UNIT_ID))));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(null, null, true, true, true, true)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
	}

	@Test
	void aclForAnotherUserIsDenied() {
		authenticate(user(USER_ID, Set.of()));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(acl(USER_ID + 1, null, true, true, true, true)));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
	}

	@Test
	void missingAclRowsAreDenied() {
		authenticate(user(USER_ID, Set.of()));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of());

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
	}

	@Test
	void nonPositiveAclIdIsDeniedWithoutRepositoryLookup() {
		authenticate(user(USER_ID, Set.of()));

		assertThat(aclAuthorizationService.canRead(0L)).isFalse();
		assertThat(aclAuthorizationService.canRead(-5L)).isFalse();
		verify(aclRepository, never()).getAclByAclId(anyLong());
	}

	@Test
	void missingAuthenticationIsDenied() {
		SecurityContextHolder.clearContext();

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
		assertThat(aclAuthorizationService.hasPrivilege(ACL_ID, AclPrivilege.READ, null)).isFalse();
		verify(aclRepository, never()).getAclByAclId(anyLong());
	}

	@Test
	void unauthenticatedTokenIsDenied() {
		var            principal      = user(USER_ID, Set.of());
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, "password");
		assertThat(authentication.isAuthenticated()).isFalse();

		assertThat(aclAuthorizationService.hasPrivilege(ACL_ID, AclPrivilege.READ, authentication)).isFalse();
		verify(aclRepository, never()).getAclByAclId(anyLong());
	}

	@Test
	void principalThatIsNotVempainUserIsDenied() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("plain-user", "password", List.of());

		assertThat(aclAuthorizationService.hasPrivilege(ACL_ID, AclPrivilege.READ, authentication)).isFalse();
		verify(aclRepository, never()).getAclByAclId(anyLong());
	}

	@Test
	void principalWithoutUserIdIsDenied() {
		authenticate(user(null, Set.of()));

		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
		verify(aclRepository, never()).getAclByAclId(anyLong());
	}

	@Test
	void anyMatchingRowIsEnough() {
		authenticate(user(USER_ID, Set.of(unit(UNIT_ID))));
		when(aclRepository.getAclByAclId(ACL_ID)).thenReturn(List.of(
				acl(USER_ID + 1, null, true, true, true, true),
				acl(USER_ID, null, false, false, false, false),
				acl(null, UNIT_ID, false, false, true, false)));

		assertThat(aclAuthorizationService.canModify(ACL_ID)).isTrue();
		assertThat(aclAuthorizationService.canRead(ACL_ID)).isFalse();
	}

	private static void authenticate(UserDetailsImpl principal) {
		SecurityContextHolder.getContext()
							 .setAuthentication(new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
	}

	private static UserDetailsImpl user(Long id, Set<Unit> units) {
		return new UserDetailsImpl(id, "login", "nick", "login@example.test", "password", units, List.of());
	}

	private static Unit unit(long id) {
		return Unit.builder()
				   .id(id)
				   .build();
	}

	private static Acl acl(Long userId, Long unitId, boolean read, boolean create, boolean modify, boolean delete) {
		return Acl.builder()
				  .aclId(ACL_ID)
				  .userId(userId)
				  .unitId(unitId)
				  .readPrivilege(read)
				  .createPrivilege(create)
				  .modifyPrivilege(modify)
				  .deletePrivilege(delete)
				  .build();
	}
}
