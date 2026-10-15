package fi.poltsi.vempain.auth.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import fi.poltsi.vempain.auth.service.UserDetailsImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
        assertTrue(token.getExpiresAt()
            .isAfter(token.getIssuedAt()));
    }

    @Test
    void generateJwtTokenFromAuthenticationOk() {
        var userDetails = new UserDetailsImpl(1L, "loginName", "nick", "email@test.com", "password", Set.of(), null);
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
    void tokenCanBeValidatedByAnotherInstanceWithTheSameSecret() {
        JwtToken token = jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com");
        var anotherJwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(anotherJwtUtils, "jwtSecret", "test-secret");

        assertTrue(anotherJwtUtils.validateJwtToken(token.getTokenString()));
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
        var otherKey = io.jsonwebtoken.Jwts.SIG.HS512.key()
            .build();
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

    @Test
    void validateJwtTokenUnsecuredTokenReturnsFalse() {
        // A token without a signature ("alg": "none") is not a signed JWT and is rejected as unsupported
        var unsecured = io.jsonwebtoken.Jwts.builder()
            .subject("login")
            .compact();
        assertFalse(jwtUtils.validateJwtToken(unsecured));
    }

    @Test
    void validateJwtTokenContentTokenReturnsFalse() throws NoSuchAlgorithmException {
        // A JWS carrying raw content instead of claims, even when signed with the configured secret, is unsupported
        var key = new SecretKeySpec(MessageDigest.getInstance("SHA-512")
            .digest("test-secret".getBytes(StandardCharsets.UTF_8)), "HmacSHA512");
        var contentToken = io.jsonwebtoken.Jwts.builder()
            .content("not claims", "text/plain")
            .signWith(key)
            .compact();
        assertFalse(jwtUtils.validateJwtToken(contentToken));
    }

    @Test
    void validateJwtTokenWithWeakVerificationKeyReturnsFalse() {
        JwtToken token = jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com");
        // Simulate a digest that yields a key far below the 512 bits HS512 requires
        var shortDigest = mock(MessageDigest.class);
        when(shortDigest.digest(any(byte[].class))).thenReturn(new byte[8]);

        try (var messageDigest = mockStatic(MessageDigest.class)) {
            messageDigest.when(() -> MessageDigest.getInstance("SHA-512"))
                .thenReturn(shortDigest);
            assertFalse(jwtUtils.validateJwtToken(token.getTokenString()));
        }
    }

    @Test
    void missingSha512FailsClosed() {
        try (var messageDigest = mockStatic(MessageDigest.class)) {
            messageDigest.when(() -> MessageDigest.getInstance("SHA-512"))
                .thenThrow(new NoSuchAlgorithmException("SHA-512"));

            var exception = assertThrows(IllegalStateException.class, () -> jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com"));
            assertEquals("SHA-512 is not available", exception.getMessage());
        }
    }

    @Test
    void blankSecretRefusesToSign() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", " ");

        var exception = assertThrows(IllegalStateException.class, () -> jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com"));
        assertEquals("The JWT signing secret is not configured", exception.getMessage());
    }

    @Test
    void missingSecretRefusesToValidate() {
        JwtToken token = jwtUtils.generateJwtTokenForUser("username", "login", "email@test.com");
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", null);

        // A missing secret is a configuration error, not an invalid token: it is not swallowed into "false"
        assertThrows(IllegalStateException.class, () -> jwtUtils.validateJwtToken(token.getTokenString()));
    }
}
