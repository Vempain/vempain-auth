package fi.poltsi.vempain.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class VempainAuthExceptionHandler {

	@ExceptionHandler(VempainAuthenticationException.class)
	public ResponseEntity<String> handleAuthenticationException(VempainAuthenticationException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
							 .body("Authentication failed");
	}

	@ExceptionHandler(VempainAclException.class)
	public ResponseEntity<String> handleAclException(VempainAclException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
							 .body(ex.getMessage());
	}

	@ExceptionHandler(VempainEntityNotFoundException.class)
	public ResponseEntity<String> handleEntityNotFoundException(VempainEntityNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
							 .body(ex.getMessage());
	}

	@ExceptionHandler(VempainRuntimeException.class)
	public ResponseEntity<String> handleRuntimeException(VempainRuntimeException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							 .body("Internal error");
	}
}
