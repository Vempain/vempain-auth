package fi.poltsi.vempain.auth.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountUnitIdUTC {

	@Test
	void getUserIdOk() {
		UserUnitId userUnitId = new UserUnitId(1L, 1L);
		assertNotNull(userUnitId);
		assertEquals(1L, userUnitId.getUserId());
	}

	@Test
	void getUnitIdOk() {
		UserUnitId userUnitId = new UserUnitId(1L, 1L);
		assertNotNull(userUnitId);
		assertEquals(1L, userUnitId.getUnitId());
	}

	@Test
	void testEqualsOk() {
		UserUnitId uui1   = new UserUnitId(1L, 1L);
		UserUnitId uui2   = new UserUnitId(1L, 1L);
		boolean    equals = uui1.equals(uui2);
		assertTrue(equals);
	}

	@Test
	void canEqualOk() {
		UserUnitId uui1 = new UserUnitId(1L, 1L);
		UserUnitId uui2 = new UserUnitId(1L, 1L);
		assertTrue(uui1.canEqual(uui2));
	}

	@Test
	void testHashCodeOk() {
		UserUnitId userUnitId = new UserUnitId(1L, 1L);
		assertNotNull(userUnitId);
		assertEquals(3541, userUnitId.hashCode());
	}
}
