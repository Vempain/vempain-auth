package fi.poltsi.vempain.auth.service;

import fi.poltsi.vempain.auth.tools.TestUTCTools;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountDetailsImplUTC {

	@Test
	void testEqualsOk() {
		var     unit         = TestUTCTools.generateUnit(1);
		var     userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", Set.of(unit), null);
		var     userDetails2 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", Set.of(unit), null);
		boolean equals       = userDetails1.equals(userDetails2);
		assertTrue(equals);
	}

	@Test
	void hashCodeOk() {
		var userDetails1 = new UserDetailsImpl(1L, "login", "nick", "email@address", "password", null, null);
		assertTrue(userDetails1.hashCode() > 0);
	}
}
