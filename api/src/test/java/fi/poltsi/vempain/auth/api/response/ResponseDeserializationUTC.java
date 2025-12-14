package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseDeserializationUTC {

	@Test
	void unitResponseDeserializes() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String json = """
				{"id":10,"name":"users","description":"Normal users"}
				""";

		UnitResponse unit = mapper.readValue(json, UnitResponse.class);

		assertNotNull(unit);
		assertEquals(10L, unit.getId());
		assertEquals("users", unit.getName());
		assertEquals("Normal users", unit.getDescription());
	}

	@Test
	void userResponseDeserializes() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		String birthday = "2000-01-01T00:00:00Z";
		String json = """
				{
				  "private_user": false,
				  "name": "Test User",
				  "nick": "testnick",
				  "login_name": "testlogin",
				  "privacy_type": "PRIVATE",
				  "email": "test@example.com",
				  "street": "Wallace st 12",
				  "pob": "01234",
				  "birthday": "%s",
				  "description": "Some desc",
				  "status": "ACTIVE"
				}
				""".formatted(birthday);

		UserResponse user = mapper.readValue(json, UserResponse.class);

		assertNotNull(user);
		assertEquals("Test User", user.getName());
		assertEquals("testnick", user.getNick());
		assertEquals("testlogin", user.getLoginName());
		assertEquals("test@example.com", user.getEmail());
		assertEquals(Instant.parse(birthday), user.getBirthday());
		assertEquals("Some desc", user.getDescription());
		assertFalse(user.isPrivateUser());
		assertEquals("Wallace st 12", user.getStreet());
		assertEquals("01234", user.getPob());
		assertEquals(PrivacyType.PRIVATE, user.getPrivacyType());
		assertEquals(AccountStatus.ACTIVE, user.getStatus());
	}

	@Test
	void jwtResponseWithUnitsDeserializes() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		String json = """
				{
				  "token": "abc",
				  "id": 1,
				  "login": "admin",
				  "nickname": "Admin",
				  "email": "admin@example.com",
				  "units": [
				    {"id":10,"name":"users","description":"Normal users"}
				  ]
				}
				""";

		LoginResponse jwt = mapper.readValue(json, LoginResponse.class);

		assertNotNull(jwt);
		assertEquals("abc", jwt.getToken());
		assertEquals(1L, jwt.getId());
		assertEquals("admin", jwt.getLogin());
		assertEquals("Admin", jwt.getNickname());
		assertEquals("admin@example.com", jwt.getEmail());
		assertNotNull(jwt.getUnits());
		assertEquals(1, jwt.getUnits()
						   .size());
		UnitResponse unit = jwt.getUnits()
							.get(0);
		assertEquals("users", unit.getName());
	}

	@Test
	void pagedResponseDeserializesWithSnakeCase() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		String json = """
				{
				  "content": [
				    {"id":10,"name":"users","description":"Normal users"},
				    {"id":20,"name":"admins","description":"Admin users"}
				  ],
				  "page": 0,
				  "size": 2,
				  "total_elements": 100,
				  "total_pages": 50,
				  "first": true,
				  "last": false,
				  "empty": false
				}
				""";

		PagedResponse<UnitResponse> pagedResponse = mapper.readValue(json, new TypeReference<PagedResponse<UnitResponse>>() {});

		assertNotNull(pagedResponse);
		assertNotNull(pagedResponse.getContent());
		assertEquals(2, pagedResponse.getContent().size());
		assertEquals(0, pagedResponse.getPage());
		assertEquals(2, pagedResponse.getSize());
		assertEquals(100L, pagedResponse.getTotalElements());
		assertEquals(50, pagedResponse.getTotalPages());
		assertTrue(pagedResponse.isFirst());
		assertFalse(pagedResponse.isLast());
		assertFalse(pagedResponse.isEmpty());

		// Verify generic type handling - check content items
		UnitResponse firstUnit = pagedResponse.getContent().get(0);
		assertEquals(10L, firstUnit.getId());
		assertEquals("users", firstUnit.getName());
		assertEquals("Normal users", firstUnit.getDescription());

		UnitResponse secondUnit = pagedResponse.getContent().get(1);
		assertEquals(20L, secondUnit.getId());
		assertEquals("admins", secondUnit.getName());
	}

	@Test
	void pagedResponseWithEmptyContentDeserializes() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		String json = """
				{
				  "content": [],
				  "page": 0,
				  "size": 10,
				  "total_elements": 0,
				  "total_pages": 0,
				  "first": true,
				  "last": true,
				  "empty": true
				}
				""";

		PagedResponse<UnitResponse> pagedResponse = mapper.readValue(json, new TypeReference<PagedResponse<UnitResponse>>() {});

		assertNotNull(pagedResponse);
		assertNotNull(pagedResponse.getContent());
		assertEquals(0, pagedResponse.getContent().size());
		assertEquals(0, pagedResponse.getPage());
		assertEquals(10, pagedResponse.getSize());
		assertEquals(0L, pagedResponse.getTotalElements());
		assertEquals(0, pagedResponse.getTotalPages());
		assertTrue(pagedResponse.isFirst());
		assertTrue(pagedResponse.isLast());
		assertTrue(pagedResponse.isEmpty());
	}

	@Test
	void pagedResponseFactoryMethodInitializesAllFields() {
		List<UnitResponse> content = new ArrayList<>();
		UnitResponse unit = new UnitResponse();
		unit.setId(10L);
		unit.setName("users");
		unit.setDescription("Normal users");
		content.add(unit);

		PagedResponse<UnitResponse> pagedResponse = PagedResponse.of(
				content,
				0,      // page
				10,     // size
				100L,   // totalElements
				10,     // totalPages
				true,   // first
				false   // last
		);

		assertNotNull(pagedResponse);
		assertEquals(content, pagedResponse.getContent());
		assertEquals(0, pagedResponse.getPage());
		assertEquals(10, pagedResponse.getSize());
		assertEquals(100L, pagedResponse.getTotalElements());
		assertEquals(10, pagedResponse.getTotalPages());
		assertTrue(pagedResponse.isFirst());
		assertFalse(pagedResponse.isLast());

		// Verify content is accessible
		assertEquals(1, pagedResponse.getContent().size());
		assertEquals("users", pagedResponse.getContent().get(0).getName());
	}

	@Test
	void pagedResponseGenericTypeHandlingWithUserResponse() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

		String birthday = "2000-01-01T00:00:00Z";
		String json = """
				{
				  "content": [
				    {
				      "private_user": false,
				      "name": "Test User",
				      "nick": "testnick",
				      "login_name": "testlogin",
				      "privacy_type": "PRIVATE",
				      "email": "test@example.com",
				      "street": "Wallace st 12",
				      "pob": "01234",
				      "birthday": "%s",
				      "description": "Some desc",
				      "status": "ACTIVE"
				    }
				  ],
				  "page": 0,
				  "size": 1,
				  "total_elements": 1,
				  "total_pages": 1,
				  "first": true,
				  "last": true,
				  "empty": false
				}
				""".formatted(birthday);

		PagedResponse<UserResponse> pagedResponse = mapper.readValue(json, new TypeReference<PagedResponse<UserResponse>>() {});

		assertNotNull(pagedResponse);
		assertEquals(1, pagedResponse.getContent().size());
		
		UserResponse user = pagedResponse.getContent().get(0);
		assertEquals("Test User", user.getName());
		assertEquals("testnick", user.getNick());
		assertEquals("testlogin", user.getLoginName());
		assertEquals(Instant.parse(birthday), user.getBirthday());
	}
}
