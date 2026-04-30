package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@SpringBootTest(classes = TestApp.class)
class UserServiceCreateITC extends IntegrationTestSetup {

	@Autowired
	private UserService userService;

	@Test
	void findByLoginOk() {
		var userId = testITCTools.generateUser("LoginPass1!");
		var user   = userService.findById(userId);
		assertTrue(user.isPresent());

		var login = user.get().getLoginName();
		var found = userService.findByLogin(login);
		assertTrue(found.isPresent());
		assertEquals(userId, found.get().getId());
	}

	@Test
	void findByLoginNotFoundReturnsEmpty() {
		var result = userService.findByLogin("nosuchlogin_xyz_" + System.currentTimeMillis());
		assertFalse(result.isPresent());
	}

	@Test
	void findUserResponseByIdOk() {
		var userId   = testITCTools.generateUser();
		var response = userService.findUserResponseById(userId);
		assertNotNull(response);
		assertEquals(userId, response.getId());
	}

	@Test
	void findUserResponseByIdNotFoundReturnsNull() {
		var response = userService.findUserResponseById(Long.MAX_VALUE);
		assertNull(response);
	}

	@Test
	void createUserOk() {
		setSecurityContext(ADMIN_ID);
		try {
			var adminAclId = testITCTools.generateAcl(ADMIN_ID, null, true, true, true, true);
			var req = UserRequest.builder()
								 .loginName("newuser_" + System.currentTimeMillis())
								 .password("ValidPass1!")
								 .name("New ITC User")
								 .nick("newuser")
								 .email("newuser" + System.currentTimeMillis() + "@test.com")
								 .privacyType(PrivacyType.PRIVATE)
								 .birthday(Instant.now().minus(10000, ChronoUnit.DAYS))
								 .acls(List.of(AclRequest.builder()
													  .aclId(adminAclId)
													  .user(ADMIN_ID)
													  .unit(null)
													  .readPrivilege(true)
													  .createPrivilege(true)
													  .modifyPrivilege(true)
													  .deletePrivilege(true)
													  .build()))
								 .build();

			var response = userService.createUser(req);
			assertNotNull(response);
			assertNotNull(response.getId());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	void updateUserOk() {
		setSecurityContext(ADMIN_ID);
		try {
			var userId     = testITCTools.generateUser();
			var user       = userService.findById(userId).orElseThrow();
			var userAclId  = user.getAclId();

			var req = UserRequest.builder()
								 .loginName(user.getLoginName())
								 .name("Updated Name")
								 .nick(user.getNick())
								 .email(user.getEmail())
								 .privacyType(PrivacyType.PRIVATE)
								 .birthday(user.getBirthday())
								 .acls(List.of(AclRequest.builder()
													  .aclId(userAclId)
													  .user(userId)
													  .unit(null)
													  .readPrivilege(true)
													  .createPrivilege(true)
													  .modifyPrivilege(true)
													  .deletePrivilege(true)
													  .build()))
								 .build();

			var response = userService.updateUser(userId, req);
			assertNotNull(response);
			assertEquals("Updated Name", response.getName());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	// ── helpers ───────────────────────────────────────────────────────────────────

	private void setSecurityContext(long userId) {
		var userDetails = new UserDetailsImpl(userId, "login", "nick", "email@test.com",
				"password", Set.of(), null);
		var auth = new UsernamePasswordAuthenticationToken(userDetails, null);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}
}
