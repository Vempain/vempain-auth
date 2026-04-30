package fi.poltsi.vempain.auth.security.jwt;

import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsUTC {

	private JwtUtils jwtUtils;

	@BeforeEach
	void setUp() {
		jwtUtils = new JwtUtils();
		ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86_400_000L);
		ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "test-secret");
	}

	@Test
	void generateJwtTokenForUserOk() {
		JwtToken token = jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com");
		assertNotNull(token);
		assertNotNull(token.getTokenString());
		assertNotNull(token.getIssuedAt());
		assertNotNull(token.getExpiresAt());
		assertTrue(token.getExpiresAt().isAfter(token.getIssuedAt()));
	}

	@Test
	void generateJwtTokenFromAuthenticationOk() {
		var userDetails = new UserDetailsImpl(1L, "loginName", "nick", "email@test.com",
				"password", Set.of(), null);
		var auth = new UsernamePasswordAuthenticationToken(userDetails, null);

		JwtToken token = jwtUtils.generateJwtToken(auth);
		assertNotNull(token);
		assertNotNull(token.getTokenString());
	}

	@Test
	void validateJwtTokenValidTokenReturnsTrue() {
		JwtToken token = jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com");
		assertTrue(jwtUtils.validateJwtToken(token.getTokenString()));
	}

	@Test
	void validateJwtTokenMalformedTokenReturnsFalse() {
		assertFalse(jwtUtils.validateJwtToken("not.a.valid.jwt.token"));
	}

	@Test
	void validateJwtTokenEmptyStringReturnsFalse() {
		assertFalse(jwtUtils.validateJwtToken(""));
	}

	@Test
	void getUserNameFromJwtTokenOk() {
		JwtToken token = jwtUtils.generateJwtTokenForUser("username", "loginName", "email@test.com");
		String login = jwtUtils.getUserNameFromJwtToken(token.getTokenString());
		assertEquals("loginName", login);
	}

	@Test
	void validateJwtTokenNullTokenReturnsFalse() {
		assertFalse(jwtUtils.validateJwtToken(null));
	}

	@Test
	void validateJwtTokenSignedWithDifferentKeyReturnsFalse() {
		// Build a token signed with a different HS512 key → should fail signature validation
		var otherKey = io.jsonwebtoken.Jwts.SIG.HS512.key().build();
		var tokenStr = io.jsonwebtoken.Jwts.builder()
										   .subject("test")
										   .signWith(otherKey)
										   .compact();
		assertFalse(jwtUtils.validateJwtToken(tokenStr));
	}

	@Test
	void validateJwtTokenExpiredTokenReturnsFalse() {
		// Use negative expiration to generate an immediately-expired token
		ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1000L);
		JwtToken token = jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com");
		assertFalse(jwtUtils.validateJwtToken(token.getTokenString()));
	}
}
