package fi.poltsi.vempain.auth.controller;

import fi.poltsi.vempain.auth.api.request.LoginRequest;
import fi.poltsi.vempain.auth.api.response.LoginResponse;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.rest.LoginAPI;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import fi.poltsi.vempain.auth.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@Slf4j
@AllArgsConstructor // This has to be all args constructor, because of the AuthenticationManager
@RestController
public class LoginController implements LoginAPI {
	private final UserService           userService;
	private       AuthenticationManager authenticationManager;
	private       JwtUtils              jwtUtils;

	@Override
	public ResponseEntity<LoginResponse> authenticateUser(LoginRequest loginRequest) {
		log.debug("LoginController::authenticateUser called");
		Authentication authentication;

		try {
			authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getLogin(), loginRequest.getPassword()));
		} catch (BadCredentialsException e) {
			log.warn("User {} attempted to log in but the authentication failed: {}", loginRequest.getLogin(), e.getMessage());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		var optionalUser = userService.findByLogin(loginRequest.getLogin());

		if (optionalUser.isEmpty()) {
			log.warn("LoginController::authenticateUser Could not find user: {}", loginRequest.getLogin());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		var user = optionalUser.get();

		if (user.isLocked()) {
			// If the user is locked, then we behave like it does not exist
			log.warn("Locked user tried to login: {}", loginRequest.getLogin());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		SecurityContextHolder.getContext()
							 .setAuthentication(authentication);
		var jwtToken = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		var             units       = userDetails.getUnits();

		var unitResponses = new ArrayList<UnitResponse>();

		for (var unit : units) {
			unitResponses.add(unit.getUnitResponse());
		}

		return ResponseEntity.ok(LoginResponse.builder()
											  .login(userDetails.getUsername())
											  .nickname(userDetails.getNickName())
											  .email(userDetails.getEmail())
											  .id(userDetails.getId())
											  .units(unitResponses)
											  .expiresAt(jwtToken.getExpiresAt())
											  .token(jwtToken.getTokenString())
											  .build());
	}
}
