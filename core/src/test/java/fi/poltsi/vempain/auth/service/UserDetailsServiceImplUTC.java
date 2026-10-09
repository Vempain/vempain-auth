package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.tools.TestUTCTools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplUTC {

	@Mock
	private fi.poltsi.vempain.auth.repository.UserAccountRepository userAccountRepository;
	@Mock
	private fi.poltsi.vempain.auth.repository.UnitRepository unitRepository;
	@Mock
	private UnitMembershipService                            unitMembershipService;

	@InjectMocks
	private UserDetailsServiceImpl userDetailsServiceImpl;

	@Test
	void loadUserByUsernameOk() {
		var unit = TestUTCTools.generateUnit(1L);
		var user = fi.poltsi.vempain.auth.entity.UserAccount.builder()
															.id(ADMIN_ID)
															.loginName("admin")
															.nick("admin")
															.email("admin@test.com")
															.password("$2a$12$hash")
															.units(Set.of(unit))
															.build();
		org.mockito.Mockito.when(userAccountRepository.findByLoginName("admin")).thenReturn(Optional.of(user));
		org.mockito.Mockito.when(unitMembershipService.findAncestorUnitIds(org.mockito.ArgumentMatchers.any()))
		                   .thenReturn(Set.of());

		var details = userDetailsServiceImpl.loadUserByUsername("admin");
		assertNotNull(details);
		assertEquals("admin", details.getUsername());
		assertEquals(Set.of(unit), ((UserDetailsImpl) details).getUnits());
	}

	@Test
	void loadUserByUsernameExpandsMembershipsThroughContainingUnits() {
		var direct      = TestUTCTools.generateUnit(3L);
		var parent      = TestUTCTools.generateUnit(2L);
		var grandParent = TestUTCTools.generateUnit(1L);
		var user = fi.poltsi.vempain.auth.entity.UserAccount.builder()
															.id(7L)
															.loginName("member")
															.nick("member")
															.email("member@test.com")
															.password("$2a$12$hash")
															.units(Set.of(direct))
															.build();
		org.mockito.Mockito.when(userAccountRepository.findByLoginName("member"))
		                   .thenReturn(Optional.of(user));
		// Unit 3 is contained in unit 2, which is contained in unit 1
		org.mockito.Mockito.when(unitMembershipService.findAncestorUnitIds(java.util.List.of(3L)))
		                   .thenReturn(Set.of(2L, 1L));
		org.mockito.Mockito.when(unitRepository.findAllById(Set.of(2L, 1L)))
		                   .thenReturn(java.util.List.of(parent, grandParent));

		var details = (UserDetailsImpl) userDetailsServiceImpl.loadUserByUsername("member");

		assertEquals(Set.of(direct, parent, grandParent), details.getUnits());
		assertEquals(3, details.getAuthorities()
		                       .size());
	}

	@Test
	void loadUserByUsernameNotFoundThrows() {
		org.mockito.Mockito.when(userAccountRepository.findByLoginName("nobody")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class,
				() -> userDetailsServiceImpl.loadUserByUsername("nobody"));
	}
}
