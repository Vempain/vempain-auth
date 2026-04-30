package fi.poltsi.vempain.auth.security.jwt;

import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import fi.poltsi.vempain.auth.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenFilterUTC {

	@Mock
	private JwtUtils               jwtUtils;
	@Mock
	private UserDetailsServiceImpl userDetailsService;
	@Mock
	private FilterChain            filterChain;

	@InjectMocks
	private AuthTokenFilter authTokenFilter;

	private JwtUtils realJwtUtils;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		realJwtUtils = new JwtUtils();
		ReflectionTestUtils.setField(realJwtUtils, "jwtExpirationMs", 86_400_000L);
		ReflectionTestUtils.setField(realJwtUtils, "jwtSecret", "test-secret");
	}

	@Test
	void doFilterInternalWithNoAuthorizationHeaderPassesThrough() throws Exception {
		var request  = new MockHttpServletRequest();
		var response = new MockHttpServletResponse();

		authTokenFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void doFilterInternalWithValidJwtSetsAuthentication() throws Exception {
		var token       = realJwtUtils.generateJwtTokenForUser("username", "loginName", "email@test.com");
		var userDetails = new UserDetailsImpl(1L, "loginName", "nick", "email@test.com",
				"password", Set.of(), List.of());

		var request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token.getTokenString());
		var response = new MockHttpServletResponse();

		when(jwtUtils.validateJwtToken(token.getTokenString())).thenReturn(true);
		when(jwtUtils.getUserNameFromJwtToken(token.getTokenString())).thenReturn("loginName");
		when(userDetailsService.loadUserByUsername("loginName")).thenReturn(userDetails);

		authTokenFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void doFilterInternalWithInvalidJwtPassesThroughWithoutAuth() throws Exception {
		var request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer invalid.jwt.token");
		var response = new MockHttpServletResponse();

		when(jwtUtils.validateJwtToken("invalid.jwt.token")).thenReturn(false);

		authTokenFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void doFilterInternalWithBearerPrefixMissingPassesThrough() throws Exception {
		var request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
		var response = new MockHttpServletResponse();

		authTokenFilter.doFilterInternal(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}
}
