package fi.poltsi.vempain.auth.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtUtils {

    @Value("${vempain.app.jwt-expiration-ms}")
    private long jwtExpirationMs;
    @Value("${vempain.app.jwt-secret}")
    private String jwtSecret;

    public JwtToken generateJwtToken(Authentication authentication) {
        var vempainUserDetails = (UserDetailsImpl) authentication.getPrincipal();
        return generateJwtTokenForUser(vempainUserDetails.getUsername(), vempainUserDetails.getLoginName(), vempainUserDetails.getEmail());
    }

    public JwtToken generateJwtTokenForUser(String username, String login, String email) {
        var nowDate = Instant.now();
        var expDate = nowDate.plus(jwtExpirationMs, ChronoUnit.MILLIS);
        // The token id is random: a JWT payload is only base64 encoded, so nothing derived from the signing secret may ever be put in it
        var jwtTokenString = Jwts.builder()
            .claim("name", username)
            .claim("email", email)
            .subject(login)
            .id(UUID.randomUUID()
                .toString())
            .issuedAt(Date.from(nowDate))
            .expiration(Date.from(expDate))
            .signWith(getSecretKey())
            .compact();
        return JwtToken.builder()
            .tokenString(jwtTokenString)
            .issuedAt(nowDate)
            .expiresAt(expDate)
            .build();
    }

    /**
     * Issues a fresh token for the user of a valid token, with the same name, login and email and the full lifetime counted from now: the sliding session of
     * the authenticated requests.
     *
     * @throws io.jsonwebtoken.JwtException when the token is not valid
     */
    public JwtToken renewJwtToken(String authToken) {
        var claims = extractAllClaims(authToken);
        return generateJwtTokenForUser(claims.get("name", String.class), claims.getSubject(), claims.get("email", String.class));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String getUserNameFromJwtToken(String authToken) {
        return extractAllClaims(authToken).getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            getJwsClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
        } catch (WeakKeyException e) {
            log.error("Weak key detected: {}", e.getMessage());
        }

        return false;
    }

    private Jws<Claims> getJwsClaims(String jwtToken) {
        return Jwts.parser()
            .verifyWith(getSecretKey())
            .build()
            .parseSignedClaims(jwtToken);
    }

    private SecretKey getSecretKey() {
        if (jwtSecret == null
            || jwtSecret.isBlank()) {
            throw new IllegalStateException("The JWT signing secret is not configured");
        }

        try {
            var keyBytes = MessageDigest.getInstance("SHA-512")
                .digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "HmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is not available", e);
        }
    }
}
