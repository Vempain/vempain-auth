package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import fi.poltsi.vempain.auth.entity.UserAccount;
import fi.poltsi.vempain.auth.exception.VempainAclException;
import fi.poltsi.vempain.auth.repository.AclRepository;
import fi.poltsi.vempain.auth.repository.UserAccountRepository;
import fi.poltsi.vempain.auth.tools.TestUTCTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceExtendedUTC {

	@Mock
	private UserAccountRepository userAccountRepository;
	@Mock
	private AclRepository         aclRepository;

	@InjectMocks
	private UserService userService;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// ── findUserResponseById ──────────────────────────────────────────────────────

	@Test
	void findUserResponseByIdFoundOk() {
		var user = buildFullUser(1L, 1L);
		when(userAccountRepository.findById(Long.valueOf(1L))).thenReturn(Optional.of(user));
		when(aclRepository.getAclByAclId(1L)).thenReturn(List.of());

		var response = userService.findUserResponseById(1L);
		assertNotNull(response);
		assertEquals(1L, response.getId());
	}

	@Test
	void findUserResponseByIdNotFoundReturnsNull() {
		when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

		var response = userService.findUserResponseById(99L);
		assertNull(response);
	}

	// ── findByLogin ───────────────────────────────────────────────────────────────

	@Test
	void findByLoginFoundOk() {
		var user = TestUTCTools.generateUser(1L);
		when(userAccountRepository.findByLoginName("testlogin")).thenReturn(Optional.of(user));

		var result = userService.findByLogin("testlogin");
		assertTrue(result.isPresent());
	}

	@Test
	void findByLoginNotFoundReturnsEmpty() {
		when(userAccountRepository.findByLoginName("nobody")).thenReturn(Optional.empty());

		var result = userService.findByLogin("nobody");
		assertTrue(result.isEmpty());
	}

	// ── save ──────────────────────────────────────────────────────────────────────

	@Test
	void saveUserOk() {
		var user = TestUTCTools.generateUser(1L);
		when(userAccountRepository.save(user)).thenReturn(user);

		var result = userService.save(user);
		assertEquals(user, result);
	}

	// ── saveAclRequests ───────────────────────────────────────────────────────────

	@Test
	void saveAclRequestsOk() {
		List<AclRequest> requests = buildAclRequests(1L, 1L, null);
		when(aclRepository.getAclByAclId(1L)).thenReturn(new ArrayList<>());
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));

		try {
			userService.saveAclRequests(1L, requests);
		} catch (VempainAclException e) {
			fail("Should not have thrown: " + e.getMessage());
		}
	}

	@Test
	void saveAclRequestsNullIdFails() {
		assertThrows(VempainAclException.class, () -> userService.saveAclRequests(null, new ArrayList<>()));
	}

	@Test
	void saveAclRequestsNegativeIdFails() {
		assertThrows(VempainAclException.class, () -> userService.saveAclRequests(-1L, new ArrayList<>()));
	}

	@Test
	void saveAclRequestsNullListFails() {
		assertThrows(VempainAclException.class, () -> userService.saveAclRequests(1L, null));
	}

	@Test
	void saveAclRequestsEmptyListFails() {
		assertThrows(VempainAclException.class, () -> userService.saveAclRequests(1L, new ArrayList<>()));
	}

	@Test
	void saveAclRequestsReplacesExistingAclsOk() {
		List<AclRequest> requests = buildAclRequests(1L, ADMIN_ID, null);
		var existingAcl = TestUTCTools.generateAcl(1L, 1L, ADMIN_ID, null);
		when(aclRepository.getAclByAclId(1L)).thenReturn(List.of(existingAcl));
		doNothing().when(aclRepository).deleteAclsByAclId(1L);
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));

		try {
			userService.saveAclRequests(1L, requests);
		} catch (VempainAclException e) {
			fail("Should not have thrown: " + e.getMessage());
		}
	}

	// ── createUser ────────────────────────────────────────────────────────────────

	@Test
	void createUserOk() {
		setSecurityContext(ADMIN_ID);

		UserRequest req = buildUserRequest("ValidPass1!", ADMIN_ID);
		when(aclRepository.getNextAclId()).thenReturn(1L);
		when(aclRepository.getAclByAclId(anyLong())).thenReturn(new ArrayList<>());
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));

		var savedUser = buildFullUser(1L, 1L);
		when(userAccountRepository.save(any())).thenReturn(savedUser);

		var response = userService.createUser(req);
		assertNotNull(response);
	}

	@Test
	void createUserInvalidPasswordThrows() {
		setSecurityContext(ADMIN_ID);

		UserRequest req = buildUserRequest("weak", ADMIN_ID);
		when(aclRepository.getNextAclId()).thenReturn(1L);
		when(aclRepository.getAclByAclId(anyLong())).thenReturn(new ArrayList<>());
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));

		var ex = assertThrows(ResponseStatusException.class, () -> userService.createUser(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void createUserBadAclThrows() {
		setSecurityContext(ADMIN_ID);

		// No ACL requests → saveAclRequests will throw VempainAclException
		var req = UserRequest.builder()
							 .loginName("testuser")
							 .password("ValidPass1!")
							 .name("Test User")
							 .nick("testy")
							 .email("test@test.com")
							 .privacyType(PrivacyType.PRIVATE)
							 .birthday(Instant.now().minusSeconds(1000))
							 .acls(new ArrayList<>())   // empty → throws
							 .build();
		when(aclRepository.getNextAclId()).thenReturn(1L);

		var ex = assertThrows(ResponseStatusException.class, () -> userService.createUser(req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	// ── updateUser ────────────────────────────────────────────────────────────────

	@Test
	void updateUserOk() {
		setSecurityContext(ADMIN_ID);

		var existingUser = buildFullUser(1L, 5L);
		when(userAccountRepository.findById(Long.valueOf(1L))).thenReturn(Optional.of(existingUser));

		UserRequest req = buildUserRequest("ValidPass1!", ADMIN_ID);
		when(aclRepository.getAclByAclId(anyLong())).thenReturn(new ArrayList<>());
		when(aclRepository.save(any())).thenAnswer(a -> a.getArgument(0));
		when(userAccountRepository.save(any())).thenReturn(existingUser);

		var response = userService.updateUser(1L, req);
		assertNotNull(response);
	}

	@Test
	void updateUserNotFoundThrows() {
		when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

		UserRequest req = buildUserRequest("ValidPass1!", ADMIN_ID);
		var ex = assertThrows(ResponseStatusException.class, () -> userService.updateUser(99L, req));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void updateUserBadAclThrows() {
		setSecurityContext(ADMIN_ID);

		var existingUser = buildFullUser(1L, 5L);
		when(userAccountRepository.findById(Long.valueOf(1L))).thenReturn(Optional.of(existingUser));

		// empty acl list → exception
		var req = UserRequest.builder()
							 .loginName("testuser")
							 .password("ValidPass1!")
							 .name("Test User")
							 .nick("testy")
							 .email("test@test.com")
							 .privacyType(PrivacyType.PRIVATE)
							 .birthday(Instant.now().minusSeconds(1000))
							 .acls(new ArrayList<>())
							 .build();

		var ex = assertThrows(ResponseStatusException.class, () -> userService.updateUser(1L, req));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	// ── helpers ───────────────────────────────────────────────────────────────────

	private void setSecurityContext(long userId) {
		var userDetails = new UserDetailsImpl(userId, "login", "nick", "email@test.com",
				"password", Set.of(), null);
		var auth = new UsernamePasswordAuthenticationToken(userDetails, null);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private UserAccount buildFullUser(long id, long aclId) {
		return UserAccount.builder()
						  .id(id)
						  .aclId(aclId)
						  .loginName("user" + id)
						  .name("User " + id)
						  .nick("user" + id)
						  .email("user" + id + "@test.com")
						  .password("$2a$12$hashedpassword")
						  .privacyType(PrivacyType.PRIVATE)
						  .birthday(Instant.now().minusSeconds(100000))
						  .status(AccountStatus.ACTIVE)
						  .locked(false)
						  .creator(ADMIN_ID)
						  .created(Instant.now())
						  .build();
	}

	private UserRequest buildUserRequest(String password, Long userId) {
		return UserRequest.builder()
						  .loginName("testuser")
						  .password(password)
						  .name("Test User")
						  .nick("testy")
						  .email("test@test.com")
						  .privacyType(PrivacyType.PRIVATE)
						  .birthday(Instant.now().minusSeconds(100000))
						  .acls(buildAclRequests(1L, userId, null))
						  .build();
	}

	private List<AclRequest> buildAclRequests(long aclId, Long userId, Long unitId) {
		return List.of(AclRequest.builder()
								 .aclId(aclId)
								 .user(userId)
								 .unit(unitId)
								 .readPrivilege(true)
								 .createPrivilege(true)
								 .modifyPrivilege(true)
								 .deletePrivilege(true)
								 .build());
	}
}
