package fi.poltsi.vempain.auth.api;

import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import fi.poltsi.vempain.auth.api.response.UnitResponse;
import fi.poltsi.vempain.auth.api.response.UserResponse;
import fi.poltsi.vempain.auth.rest.AclAPI;
import fi.poltsi.vempain.auth.rest.UnitAPI;
import fi.poltsi.vempain.auth.rest.UserAPI;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON contract of the unit membership fields and the paths of the management APIs consumed by every SPA.
 */
class UnitMembershipContractJTC {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void managementPathsRemainStable() {
		assertEquals("/content-management/users", UserAPI.MAIN_PATH);
		assertEquals("/content-management/units", UnitAPI.MAIN_PATH);
		assertEquals("/content-management/acls", AclAPI.MAIN_PATH);
	}

	@Test
	void unitMembersAreSnakeCaseAndOptional() throws Exception {
		var request = objectMapper.readValue("{\"name\": \"a\", \"description\": \"d\", \"user_ids\": [3, 7], \"unit_ids\": [12]}", UnitRequest.class);
		assertEquals(List.of(3L, 7L), request.getUserIds());
		assertEquals(List.of(12L), request.getUnitIds());

		var withoutMembers = objectMapper.readValue("{\"name\": \"a\"}", UnitRequest.class);
		assertNull(withoutMembers.getUserIds(), "a missing list leaves the memberships untouched");
		assertNull(withoutMembers.getUnitIds());

		var response = UnitResponse.builder()
		                           .name("a")
		                           .userIds(List.of(3L))
		                           .unitIds(List.of(12L))
		                           .build();
		response.setId(5L);
		var json = objectMapper.writeValueAsString(response);
		assertTrue(json.contains("\"user_ids\":[3]"));
		assertTrue(json.contains("\"unit_ids\":[12]"));
		assertTrue(!json.contains("userIds"));
	}

	@Test
	void userUnitMembershipsAreSnakeCase() throws Exception {
		var request = objectMapper.readValue("{\"login_name\": \"x\", \"unit_ids\": [12, 13]}", UserRequest.class);
		assertEquals(List.of(12L, 13L), request.getUnitIds());

		var response = UserResponse.builder()
		                           .loginName("x")
		                           .unitIds(List.of(12L))
		                           .build();
		response.setId(9L);
		var json = objectMapper.writeValueAsString(response);
		assertTrue(json.contains("\"unit_ids\":[12]"));
		assertTrue(json.contains("\"login_name\":\"x\""));
	}
}
