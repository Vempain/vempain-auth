package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.entity.Acl;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UnitRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AclService2UTC {

	AclRepository         aclRepository         = mock(AclRepository.class);
	UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
	UnitRepository        unitRepository        = mock(UnitRepository.class);
	AclIdGenerator        aclIdGenerator        = mock(AclIdGenerator.class);

	AclService service;

	@BeforeEach
	void setUp() {
		service = new AclService(aclRepository, userAccountRepository, unitRepository, aclIdGenerator);
	}

	@Test
	void createUniqueAclCallsCustomInsert() throws VempainAclException {
		Acl toReturn = Acl.builder()
						  .id(10L)
						  .aclId(123L)
						  .userId(2L)
						  .unitId(null)
						  .createPrivilege(true)
						  .readPrivilege(true)
						  .modifyPrivilege(true)
						  .deletePrivilege(true)
						  .build();
		when(aclRepository.insertWithNextAclId(any())).thenReturn(toReturn);
		// Mock both overloads (primitive long and boxed Long) if present
		when(userAccountRepository.findById(Mockito.eq(2L))).thenReturn(Optional.of(new UserAccount()));
		try {
			when(userAccountRepository.findById(Mockito.eq(Long.valueOf(2L)))).thenReturn(Optional.of(new UserAccount()));
		} catch (Throwable ignored) {
		}
		when(userAccountRepository.findById(Mockito.anyLong())).thenReturn(Optional.of(new UserAccount()));

		// sanity check that mock is returning expected value
		assertTrue(userAccountRepository.findById(2L)
										.isPresent());

		Acl result = service.createUniqueAcl(2L, null, true, true, true, true);

		assertNotNull(result);
		assertEquals(123L, result.getAclId());
		ArgumentCaptor<Acl> captor = ArgumentCaptor.forClass(Acl.class);
		verify(aclRepository, times(1)).insertWithNextAclId(captor.capture());
		Acl passed = captor.getValue();
		assertEquals(2L, passed.getUserId());
		assertTrue(passed.isReadPrivilege());
	}
}
