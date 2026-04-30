package fi.poltsi.vempain.auth.security.jwt;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthEntryPointJwtUTC {

	@Test
	void commenceReturnsUnauthorized() throws Exception {
		var entryPoint = new AuthEntryPointJwt();
		var request    = new MockHttpServletRequest();
		var response   = new MockHttpServletResponse();

		AuthenticationException authException = new AuthenticationException("Unauthorized") {
		};

		entryPoint.commence(request, response, authException);

		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
	}
}
