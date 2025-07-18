package fi.poltsi.vempain.auth.security.jwt;

import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

	@Value("${vempain.app.jwtExpirationMs}")
	private       long      jwtExpirationMs;
	// TODO This needs to be cleaned up as the secret is not used at all for the moment
	@Value("${vempain.app.jwtSecret}")
	private       String    jwtSecret;
	private final SecretKey secretKey = Jwts.SIG.HS512.key().build();

	public String generateJwtToken(Authentication authentication) {
		var vempainUserDetails = (UserDetailsImpl) authentication.getPrincipal();
		return generateJwtTokenForUser(vempainUserDetails.getUsername(), vempainUserDetails.getLoginName(), vempainUserDetails.getEmail());
	}

	public String generateJwtTokenForUser(String username, String login, String email) {
		if (secretKey == null) {
			log.error("The JWT signing key is null");
			return null;
		}

		var nowDate = Instant.now();
		var expDate = nowDate.plus(jwtExpirationMs, ChronoUnit.MILLIS);
		return Jwts.builder()
				   .claim("name", username)
				   .claim("email", email)
				   .subject(login)
				   .id(UUID.randomUUID().toString())
				   .issuedAt(Date.from(nowDate))
				   .expiration(Date.from(expDate))
				   .signWith(secretKey)
				   .compact();
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
				   .verifyWith(secretKey)
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
		} catch (WeakKeyException e) {
			log.error("Weak key detected: {}", e.getMessage());
		}

		return false;
	}

	private Jws<Claims> getJwsClaims(String jwtToken) {
		return Jwts.parser()
				   .verifyWith(secretKey)
				   .build()
				   .parseSignedClaims(jwtToken);
	}
}
