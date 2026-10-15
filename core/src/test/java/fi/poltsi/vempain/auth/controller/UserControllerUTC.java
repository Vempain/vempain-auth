package fi.poltsi.vempain.auth.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The guard clauses of {@link UserController#findById}: a malformed id never reaches the service and a missing user is a 404.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerUTC {

    @Mock
    private UserService userService;
    @Mock
    private AdministrationGuard administrationGuard;

    @InjectMocks
    private UserController userController;

    @Test
    void negativeUserIdIsBadRequest() {
        var exception = assertThrows(ResponseStatusException.class, () -> userController.findById(-1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Malformed parameter", exception.getReason());
        verifyNoInteractions(userService);
    }

    @Test
    void missingUserIdIsBadRequest() {
        var exception = assertThrows(ResponseStatusException.class, () -> userController.findById(null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(userService);
    }

    @Test
    void unknownUserIsNotFound() {
        when(userService.findUserResponseById(42L)).thenReturn(null);

        var exception = assertThrows(ResponseStatusException.class, () -> userController.findById(42L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("No user was found with given ID", exception.getReason());
    }

    @Test
    void existingUserIsReturned() {
        var user = UserResponse.builder()
            .id(7L)
            .nick("seven")
            .build();
        when(userService.findUserResponseById(7L)).thenReturn(user);

        var response = userController.findById(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
    }
}
