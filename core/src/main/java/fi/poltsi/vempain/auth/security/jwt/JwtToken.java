package fi.poltsi.vempain.auth.security.jwt;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class JwtToken {

    private String tokenString;
    private Instant issuedAt;
    private Instant expiresAt;
}
