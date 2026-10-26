package fi.poltsi.vempain.auth.security.jwt;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import fi.poltsi.vempain.auth.api.Constants;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates a request by its bearer JWT. An authenticated request that the user triggered also renews the session: the response carries a fresh token in
 * {@link Constants#RENEWED_TOKEN_HEADER} (expiry in {@link Constants#RENEWED_TOKEN_EXPIRES_AT_HEADER}), so the session lifetime is counted from the user's last
 * action. Requests marked with {@link Constants#BACKGROUND_REQUEST_HEADER} (polling) are served but not renewed.
 */
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (jwt != null
                && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
                if (!isBackgroundRequest(request)) {
                    renewSession(jwt, response);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication:", e);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Answers with a fresh token for the same user; a failure to renew is logged and leaves the request authenticated with its current token.
     */
    private void renewSession(String jwt, HttpServletResponse response) {
        try {
            var renewed = jwtUtils.renewJwtToken(jwt);
            response.setHeader(Constants.RENEWED_TOKEN_HEADER, renewed.getTokenString());
            response.setHeader(Constants.RENEWED_TOKEN_EXPIRES_AT_HEADER, renewed.getExpiresAt()
                .toString());
        } catch (Exception e) {
            log.warn("The session token could not be renewed: {}", e.getMessage());
        }
    }

    static boolean isBackgroundRequest(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader(Constants.BACKGROUND_REQUEST_HEADER));
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth)
            && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
