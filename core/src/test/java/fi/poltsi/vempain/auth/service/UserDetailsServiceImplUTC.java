package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.Unit;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplUTC {

	@Mock
	private fi.poltsi.vempain.auth.repository.UserAccountRepository userAccountRepository;

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

		var details = userDetailsServiceImpl.loadUserByUsername("admin");
		assertNotNull(details);
		assertEquals("admin", details.getUsername());
	}

	@Test
	void loadUserByUsernameNotFoundThrows() {
		org.mockito.Mockito.when(userAccountRepository.findByLoginName("nobody")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class,
				() -> userDetailsServiceImpl.loadUserByUsername("nobody"));
	}
}
