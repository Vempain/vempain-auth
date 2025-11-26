package fi.poltsi.vempain.auth.api.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.poltsi.vempain.auth.api.AccountStatus;
import fi.poltsi.vempain.auth.api.PrivacyType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
		assertEquals(false, user.isPrivateUser());
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

		JwtResponse jwt = mapper.readValue(json, JwtResponse.class);

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
}
