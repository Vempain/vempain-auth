package fi.poltsi.vempain.auth.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VempainAuthExceptionHandlerUTC {

	private VempainAuthExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new VempainAuthExceptionHandler();
	}

	@Test
	void handleAuthenticationExceptionReturns401() {
		var ex       = new VempainAuthenticationException();
		var response = handler.handleAuthenticationException(ex);
		assertNotNull(response);
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		assertEquals("Authentication failed", response.getBody());
	}

	@Test
	void handleAclExceptionReturns403() {
		var ex       = new VempainAclException("access denied");
		var response = handler.handleAclException(ex);
		assertNotNull(response);
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertEquals("access denied", response.getBody());
	}

	@Test
	void handleEntityNotFoundExceptionReturns404() {
		var ex       = new VempainEntityNotFoundException("not found message", "Unit");
		var response = handler.handleEntityNotFoundException(ex);
		assertNotNull(response);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("not found message", response.getBody());
	}

	@Test
	void handleRuntimeExceptionReturns400() {
		var ex       = new VempainRuntimeException();
		var response = handler.handleRuntimeException(ex);
		assertNotNull(response);
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("Internal error", response.getBody());
	}
}
