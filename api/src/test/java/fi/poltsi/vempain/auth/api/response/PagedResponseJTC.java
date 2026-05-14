package fi.poltsi.vempain.auth.api.response;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
class PagedResponseJTC {

	@Autowired
	private ObjectMapper mapper;

	@Test
	void pagedResponseSerializesPaginationMetadataWithSnakeCaseKeys() throws Exception {
		PagedResponse<String> response = PagedResponse.of(
				List.of("one", "two"),
				3,
				25,
				70000L,
				2800,
				false,
				false
		);

		JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

		assertTrue(json.has("content"));
		assertTrue(json.has("page"));
		assertTrue(json.has("size"));
		assertTrue(json.has("total_pages"));
		assertTrue(json.has("total_elements"));
		assertTrue(json.has("first"));
		assertTrue(json.has("last"));
		assertTrue(json.has("empty"));
		assertFalse(json.has("totalPages"));
		assertFalse(json.has("totalElements"));

		assertEquals(3, json.get("page")
		                    .asInt());
		assertEquals(25, json.get("size")
		                     .asInt());
		assertEquals(70000L, json.get("total_elements")
		                         .asLong());
		assertEquals(2800, json.get("total_pages")
		                       .asInt());
		assertFalse(json.get("first")
		                .asBoolean());
		assertFalse(json.get("last")
		                .asBoolean());
		assertFalse(json.get("empty")
		                .asBoolean());
	}

	@Test
	void pagedResponseSerializesEmptyContentAsEmptyTrue() throws Exception {
		PagedResponse<String> response = PagedResponse.of(
				List.of(),
				0,
				50,
				0L,
				0,
				true,
				true
		);

		JsonNode json = mapper.readTree(mapper.writeValueAsString(response));

		assertTrue(json.has("empty"));
		assertTrue(json.get("empty")
		               .asBoolean());
		assertEquals(0, json.get("total_pages")
		                    .asInt());
		assertEquals(0L, json.get("total_elements")
		                     .asLong());
	}

	@Test
	void pagedResponseDeserializesSnakeCaseKeys() throws Exception {
		String json = """
				{
				  "content": ["one"],
				  "page": 1,
				  "size": 10,
				  "total_elements": 31,
				  "total_pages": 4,
				  "first": false,
				  "last": false,
				  "empty": false
				}
				""";

		PagedResponse<String> response = mapper.readValue(json, mapper.getTypeFactory()
		                                                              .constructParametricType(PagedResponse.class, String.class));

		assertEquals(1, response.getPage());
		assertEquals(10, response.getSize());
		assertEquals(31L, response.getTotalElements());
		assertEquals(4, response.getTotalPages());
		assertFalse(response.isFirst());
		assertFalse(response.isLast());
		assertFalse(response.isEmpty());
	}

	@Test
	void pagedResponseDeserializesSnakeCaseKeysWithFactoryCompatibleValues() throws Exception {
		String json = """
				{
				  "content": ["legacy"],
				  "page": 0,
				  "size": 10,
				  "total_elements": 1,
				  "total_pages": 1,
				  "first": true,
				  "last": true,
				  "empty": false
				}
				""";

		PagedResponse<String> response = mapper.readValue(json, mapper.getTypeFactory()
		                                                              .constructParametricType(PagedResponse.class, String.class));

		assertEquals(1L, response.getTotalElements());
		assertEquals(1, response.getTotalPages());
		assertTrue(response.isFirst());
		assertTrue(response.isLast());
	}

	// Provides the minimal Spring Boot configuration required for @JsonTest in the api module.
	@SpringBootConfiguration
	public static class TestConfig {
	}
}
