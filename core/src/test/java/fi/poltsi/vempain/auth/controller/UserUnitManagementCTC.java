package fi.poltsi.vempain.auth.controller;

import fi.poltsi.vempain.auth.IntegrationTestSetup;
import fi.poltsi.vempain.auth.TestApp;
import fi.poltsi.vempain.auth.api.PrivacyType;
import fi.poltsi.vempain.auth.api.request.AclRequest;
import fi.poltsi.vempain.auth.api.request.PagedRequest;
import fi.poltsi.vempain.auth.api.request.UnitRequest;
import fi.poltsi.vempain.auth.api.request.UserRequest;
import fi.poltsi.vempain.auth.entity.Unit;
import fi.poltsi.vempain.auth.security.jwt.JwtUtils;
import fi.poltsi.vempain.auth.service.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static fi.poltsi.vempain.auth.api.Constants.ADMIN_ID;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller Test Class (CTC) for the user, unit and ACL management endpoints the library hosts in every service
 * ({@code /content-management/users|units|acls}): administrator-only, snake_case contract, member handling and the cycle backstop.
 */
@SpringBootTest(classes = TestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserUnitManagementCTC extends IntegrationTestSetup {
	@Autowired
	private MockMvc      mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@MockitoBean
	private JwtUtils     jwtUtils;

	private RequestPostProcessor admin() {
		var account = userAccountRepository.findById(ADMIN_ID)
										   .orElseThrow();
		return user(UserDetailsImpl.build(account));
	}

	/**
	 * A user that exists but holds no row on the administrator ACL.
	 */
	private RequestPostProcessor plainUser() {
		var account = userAccountRepository.findById(testITCTools.generateUser())
										   .orElseThrow();
		return user(UserDetailsImpl.build(account));
	}

	private static AclRequest adminAcl() {
		return AclRequest.builder()
						 .user(ADMIN_ID)
						 .readPrivilege(true)
						 .createPrivilege(true)
						 .modifyPrivilege(true)
						 .deletePrivilege(true)
						 .build();
	}

	private long unit(String name) {
		var aclId = testITCTools.generateAcl(ADMIN_ID, null, true, true, true, true);
		return unitService.save(Unit.builder()
									.name(name)
									.description("CTC " + name)
									.aclId(aclId)
									.locked(false)
									.creator(ADMIN_ID)
									.created(Instant.now())
									.build())
						  .getId();
	}

	@Test
	void endpointsRequireAnAuthenticatedAdministrator() throws Exception {
		mockMvc.perform(get("/content-management/users"))
			   .andExpect(status().isUnauthorized());
		mockMvc.perform(get("/content-management/units"))
			   .andExpect(status().isUnauthorized());
		mockMvc.perform(get("/content-management/acls"))
			   .andExpect(status().isUnauthorized());

		mockMvc.perform(get("/content-management/users").with(plainUser()))
			   .andExpect(status().isOk());
		mockMvc.perform(get("/content-management/users/" + ADMIN_ID).with(plainUser()))
			   .andExpect(status().isOk());
		mockMvc.perform(get("/content-management/units").with(plainUser()))
			   .andExpect(status().isOk());
		var paged = new PagedRequest();
		paged.setPage(0);
		paged.setSize(10);
		mockMvc.perform(post("/content-management/users/paged").with(plainUser())
															   .contentType(MediaType.APPLICATION_JSON)
															   .content(objectMapper.writeValueAsString(paged)))
			   .andExpect(status().isOk());
		mockMvc.perform(post("/content-management/units/paged").with(plainUser())
															   .contentType(MediaType.APPLICATION_JSON)
															   .content(objectMapper.writeValueAsString(paged)))
			   .andExpect(status().isOk());
		mockMvc.perform(get("/content-management/acls").with(plainUser()))
			   .andExpect(status().isForbidden());
	}

	@Test
	void usersAreListedPagedCreatedAndUpdatedWithTheirUnits() throws Exception {
		var unitId = unit("writers");

		var created = UserRequest.builder()
								 .loginName("ctc.user")
								 .name("CTC User")
								 .nick("ctc")
								 .email("ctc.user@test.tld")
								 .password("S3cure-Pass!")
								 .privacyType(PrivacyType.PRIVATE)
								 .birthday(Instant.parse("1990-01-01T00:00:00Z"))
								 .acls(List.of(adminAcl()))
								 .unitIds(List.of(unitId))
								 .build();
		var createdJson = mockMvc.perform(post("/content-management/users").with(admin())
																		   .contentType(MediaType.APPLICATION_JSON)
																		   .content(objectMapper.writeValueAsString(created)))
								 .andExpect(status().isOk())
								 .andExpect(jsonPath("$.login_name").value("ctc.user"))
								 .andExpect(jsonPath("$.unit_ids[0]").value(unitId))
								 .andExpect(jsonPath("$.acls", hasSize(1)))
								 .andReturn()
								 .getResponse()
								 .getContentAsString();
		var userId = objectMapper.readTree(createdJson)
								 .path("id")
								 .asLong();

		mockMvc.perform(get("/content-management/users").with(admin()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$[?(@.login_name == 'ctc.user')].unit_ids[0]", hasItem((int) unitId)));

		var paged = new PagedRequest();
		paged.setPage(0);
		paged.setSize(10);
		paged.setSearch("ctc");
		mockMvc.perform(post("/content-management/users/paged").with(admin())
															   .contentType(MediaType.APPLICATION_JSON)
															   .content(objectMapper.writeValueAsString(paged)))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.total_elements").value(1))
			   .andExpect(jsonPath("$.content[0].login_name").value("ctc.user"));

		mockMvc.perform(get("/content-management/users/" + userId).with(admin()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.name").value("CTC User"))
			   .andExpect(jsonPath("$.unit_ids[0]").value(unitId));
		mockMvc.perform(get("/content-management/users/987654").with(admin()))
			   .andExpect(status().isNotFound());

		var updated = UserRequest.builder()
								 .loginName("ctc.user")
								 .name("CTC User Renamed")
								 .nick("ctc")
								 .email("ctc.user@test.tld")
								 .privacyType(PrivacyType.PRIVATE)
								 .birthday(Instant.parse("1990-01-01T00:00:00Z"))
								 .acls(List.of(adminAcl()))
								 .unitIds(List.of())
								 .build();
		mockMvc.perform(put("/content-management/users/" + userId).with(admin())
																  .contentType(MediaType.APPLICATION_JSON)
																  .content(objectMapper.writeValueAsString(updated)))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.name").value("CTC User Renamed"))
			   .andExpect(jsonPath("$.unit_ids", hasSize(0)));
	}

	@Test
	void unitsCarryMembersAndRejectCircularNesting() throws Exception {
		var userId = testITCTools.generateUser();
		var inner  = unit("inner");

		var created = UnitRequest.builder()
								 .name("outer")
								 .description("contains inner")
								 .acls(List.of(adminAcl()))
								 .userIds(List.of(userId))
								 .unitIds(List.of(inner))
								 .build();
		var createdJson = mockMvc.perform(post("/content-management/units").with(admin())
																		   .contentType(MediaType.APPLICATION_JSON)
																		   .content(objectMapper.writeValueAsString(created)))
								 .andExpect(status().isOk())
								 .andExpect(jsonPath("$.name").value("outer"))
								 .andExpect(jsonPath("$.user_ids[0]").value(userId))
								 .andExpect(jsonPath("$.unit_ids[0]").value(inner))
								 .andReturn()
								 .getResponse()
								 .getContentAsString();
		var outer = objectMapper.readTree(createdJson)
								.path("id")
								.asLong();

		mockMvc.perform(get("/content-management/units/" + outer).with(admin()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.unit_ids[0]").value(inner))
			   .andExpect(jsonPath("$.acls", hasSize(1)));
		mockMvc.perform(get("/content-management/units/" + outer).with(plainUser()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.unit_ids[0]").value(inner));

		var paged = new PagedRequest();
		paged.setPage(0);
		paged.setSize(50);
		mockMvc.perform(post("/content-management/units/paged").with(admin())
															   .contentType(MediaType.APPLICATION_JSON)
															   .content(objectMapper.writeValueAsString(paged)))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.content[?(@.name == 'outer')].unit_ids[0]", hasItem((int) inner)));

		// inner may not contain outer, which contains inner
		var circular = UnitRequest.builder()
								  .name("inner")
								  .description("would close the loop")
								  .acls(List.of(adminAcl()))
								  .unitIds(List.of(outer))
								  .build();
		mockMvc.perform(put("/content-management/units/" + inner).with(admin())
																 .contentType(MediaType.APPLICATION_JSON)
																 .content(objectMapper.writeValueAsString(circular)))
			   .andExpect(status().isBadRequest());
		mockMvc.perform(get("/content-management/units/" + inner).with(admin()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.unit_ids", hasSize(0)));

		// Removing the members through the update
		var emptied = UnitRequest.builder()
								 .name("outer")
								 .description("emptied")
								 .acls(List.of(adminAcl()))
								 .userIds(List.of())
								 .unitIds(List.of())
								 .build();
		mockMvc.perform(put("/content-management/units/" + outer).with(admin())
																 .contentType(MediaType.APPLICATION_JSON)
																 .content(objectMapper.writeValueAsString(emptied)))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.user_ids", hasSize(0)))
			   .andExpect(jsonPath("$.unit_ids", hasSize(0)));
	}

	@Test
	void aclRowsAreListedForAdministrators() throws Exception {
		var aclId = testITCTools.generateAcl(ADMIN_ID, null, true, false, false, false);

		mockMvc.perform(get("/content-management/acls").with(admin()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$[?(@.acl_id == " + aclId + ")]", hasSize(1)));
		mockMvc.perform(get("/content-management/acls/" + aclId).with(admin()))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$[0].acl_id").value(aclId))
			   .andExpect(jsonPath("$[0].read_privilege").value(true));
		mockMvc.perform(get("/content-management/acls/987654").with(admin()))
			   .andExpect(status().isNotFound());
	}
}
