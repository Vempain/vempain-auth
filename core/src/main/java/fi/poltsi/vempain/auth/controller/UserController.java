package fi.poltsi.vempain.auth.controller;

import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import fi.poltsi.vempain.auth.api.response.PagedResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.rest.UserAPI;
import fi.poltsi.vempain.auth.security.AdministrationGuard;
import fi.poltsi.vempain.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * User account management hosted by every service that includes {@code vempain-auth-core}; it manages that service's own user base.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class UserController implements UserAPI {
	private final UserService         userService;
	private final AdministrationGuard administrationGuard;

	@Override
	public ResponseEntity<List<UserResponse>> getUsers() {
		administrationGuard.requireAdministrator();
		return ResponseEntity.ok(userService.findAllResponses());
	}

	@Override
	public ResponseEntity<PagedResponse<UserResponse>> getPagedUsers(PagedRequest request) {
		administrationGuard.requireAdministrator();
		return ResponseEntity.ok(userService.findPaged(request));
	}

	@Override
	public ResponseEntity<UserResponse> findById(Long userId) {
		administrationGuard.requireAdministrator();
		if (userId == null || userId < 0) {
			log.error("Invalid user ID: {}", userId);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed parameter");
		}

		var userResponse = userService.findUserResponseById(userId);

		if (userResponse == null) {
			log.error("Could not find any user by id {}", userId);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user was found with given ID");
		}

		return ResponseEntity.ok(userResponse);
	}

	@Override
	public ResponseEntity<UserResponse> addUser(UserRequest userRequest) {
		administrationGuard.requireAdministrator();
		return ResponseEntity.ok(userService.createUser(userRequest));
	}

	@Override
	public ResponseEntity<UserResponse> updateUser(Long userId, UserRequest userRequest) {
		administrationGuard.requireAdministrator();
		return ResponseEntity.ok(userService.updateUser(userId, userRequest));
	}
}
