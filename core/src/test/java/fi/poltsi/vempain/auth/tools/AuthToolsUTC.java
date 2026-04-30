package fi.poltsi.vempain.auth.tools;

import fi.poltsi.vempain.auth.exception.VempainAuthenticationException;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthToolsUTC {

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// ── passwordCheck ────────────────────────────────────────────────────────────

	@Test
	void passwordCheckTooShortFails() {
		assertFalse(AuthTools.passwordCheck("Aa1!"));
	}

	@Test
	void passwordCheckNoUppercaseFails() {
		assertFalse(AuthTools.passwordCheck("lowercase1!abcdef"));
	}

	@Test
	void passwordCheckNoLowercaseFails() {
		assertFalse(AuthTools.passwordCheck("UPPERCASE1!ABCDEF"));
	}

	@Test
	void passwordCheckNoDigitFails() {
		assertFalse(AuthTools.passwordCheck("Lowercase!ABCDEFG"));
	}

	@Test
	void passwordCheckNoSpecialCharFails() {
		assertFalse(AuthTools.passwordCheck("Lowercase1ABCDEFG"));
	}

	@Test
	void passwordCheckValidPasswordOk() {
		assertTrue(AuthTools.passwordCheck("ValidPass1!"));
	}

	// ── passwordHash ─────────────────────────────────────────────────────────────

	@Test
	void passwordHashProducesNonNullResult() {
		String hash = AuthTools.passwordHash("ValidPass1!");
		assertNotNull(hash);
		assertTrue(hash.startsWith("$2a$"));
	}

	// ── getCurrentUserId ─────────────────────────────────────────────────────────

	@Test
	void getCurrentUserIdWithAuthenticatedUserOk() {
		setSecurityContext(42L);
		long userId = AuthTools.getCurrentUserId();
		assertEquals(42L, userId);
	}

	@Test
	void getCurrentUserIdWithoutAuthenticationThrows() {
		// No context set – getAuthentication() returns null → throws
		assertThrows(VempainAuthenticationException.class, AuthTools::getCurrentUserId);
	}

	@Test
	void getCurrentUserIdWithStringPrincipalThrows() {
		// Spring sometimes puts an anonymousUser string principal
		var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
		SecurityContextHolder.getContext().setAuthentication(auth);
		assertThrows(VempainAuthenticationException.class, AuthTools::getCurrentUserId);
	}

	// ── isUserIdCurrentUser ───────────────────────────────────────────────────────

	@Test
	void isUserIdCurrentUserMatchingIdReturnsTrue() {
		setSecurityContext(7L);
		assertTrue(AuthTools.isUserIdCurrentUser(7L));
	}

	@Test
	void isUserIdCurrentUserDifferentIdReturnsFalse() {
		setSecurityContext(7L);
		assertFalse(AuthTools.isUserIdCurrentUser(99L));
	}

	@Test
	void isUserIdCurrentUserWithNoAuthReturnsFalse() {
		// No auth set → returns false
		assertFalse(AuthTools.isUserIdCurrentUser(1L));
	}

	// ── helpers ──────────────────────────────────────────────────────────────────

	private void setSecurityContext(long userId) {
		var userDetails = new UserDetailsImpl(userId, "login", "nick", "email@test.com", "password",
				Set.of(), null);
		var auth = new UsernamePasswordAuthenticationToken(userDetails, null);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}
}
