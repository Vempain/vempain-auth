package fi.poltsi.vempain.auth.controller;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import fi.poltsi.vempain.auth.api.request.LoginRequest;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The branch of {@link LoginController#authenticateUser} that the container tests can not reach: the authentication manager accepts the credentials but the
 * account can not be loaded afterwards. No token is issued and nothing is left in the security context.
 */
@ExtendWith(MockitoExtension.class)
class LoginControllerUTC {

    @Mock
    private UserService userService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private LoginController loginController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedLoginWithoutAnAccountIsNotFound() {
        var request = LoginRequest.builder()
            .login("ghost")
            .password("S3cure-Pass!")
            .build();
        when(authenticationManager.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken("ghost", null, List.of()));
        when(userService.findByLogin("ghost")).thenReturn(Optional.empty());

        var response = loginController.authenticateUser(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verifyNoInteractions(jwtUtils);
        assertNull(SecurityContextHolder.getContext()
            .getAuthentication());
    }
}
