package fi.poltsi.vempain.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
public class VempainGlobalExceptionHandlerITC {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void testHandleAuthenticationException() throws Exception {
		mockMvc.perform(get("/test-authentication-exception"))
			   .andExpect(status().isUnauthorized())
			   .andExpect(content().string("Authentication failed"));
	}

	@Test
	void testHandleAclException() throws Exception {
		mockMvc.perform(get("/test-acl-exception"))
			   .andExpect(status().isForbidden())
			   .andExpect(content().string("Access denied"));
	}

	@Test
	void testHandleEntityNotFoundException() throws Exception {
		mockMvc.perform(get("/test-entity-not-found-exception"))
			   .andExpect(status().isNotFound())
			   .andExpect(content().string("The object was not found"));
	}

	@Test
	void testHandleRuntimeException() throws Exception {
		mockMvc.perform(get("/test-runtime-exception"))
			   .andExpect(status().isInternalServerError())
			   .andExpect(content().string("Internal error"));
	}
}
