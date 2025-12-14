package fi.poltsi.vempain.auth.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class JwtToken {
	private String  tokenString;
	private Instant issuedAt;
	private Instant expiresAt;
}
