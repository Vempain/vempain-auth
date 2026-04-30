package fi.poltsi.vempain.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VempainExceptionsUTC {

	@Test
	void vempainAbstractExceptionMessageOk() {
		VempainAbstractException ex = new VempainAbstractException("abstract message");
		assertNotNull(ex);
		assertEquals("abstract message", ex.getMessage());
	}

	@Test
	void vempainAclExceptionMessageOk() {
		VempainAclException ex = new VempainAclException("acl message");
		assertNotNull(ex);
		assertEquals("acl message", ex.getMessage());
	}

	@Test
	void vempainEntityNotFoundExceptionWithArgsOk() {
		VempainEntityNotFoundException ex = new VempainEntityNotFoundException("entity message", "Unit");
		assertNotNull(ex);
		assertEquals("entity message", ex.getMessage());
		assertEquals("Unit", ex.getEntityName());
	}

	@Test
	void vempainEntityNotFoundExceptionNoArgsOk() {
		VempainEntityNotFoundException ex = new VempainEntityNotFoundException();
		assertNotNull(ex);
		assertEquals("The object was not found", ex.getMessage());
		assertEquals("", ex.getEntityName());
	}

	@Test
	void vempainAuthenticationExceptionOk() {
		VempainAuthenticationException ex = new VempainAuthenticationException();
		assertNotNull(ex);
	}

	@Test
	void vempainRuntimeExceptionOk() {
		VempainRuntimeException ex = new VempainRuntimeException();
		assertNotNull(ex);
	}
}
