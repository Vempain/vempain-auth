package fi.poltsi.vempain.auth.tools;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class JsonToolsUTC {

	@Test
	void toJsonSerializesPojoAndMapWithMasking() throws Exception {
		record Simple(String name, int age) {
		}

		Simple   valid      = new Simple("Alice", 30);
		JsonNode simpleNode = new ObjectMapper().readTree(JsonTools.toJson(valid));
		assertEquals("Alice", simpleNode.get("name")
										.asString());
		assertEquals(30, simpleNode.get("age")
								   .asInt());

		Map<String, String> payload    = Map.of("secret", "topsecret", "visible", "ok");
		var                 jsonString = JsonTools.toJson(payload, List.of("secret"));
		log.info("Masked JSON: {}", jsonString);
		JsonNode masked = new ObjectMapper().readTree(jsonString);
		assertEquals("to*et", masked.get("secret")
									.asString());
		assertEquals("ok", masked.get("visible")
								 .asString());
	}

	@Test
	void toJsonHandlesNullValuesInMap() throws Exception {
		// null value in map must be returned as null without NPE
		Map<String, Object> payload = new java.util.HashMap<>();
		payload.put("key", null);
		payload.put("visible", "ok");
		var jsonString = JsonTools.toJson(payload, List.of("key"));
		JsonNode node = new ObjectMapper().readTree(jsonString);
		assertTrue(node.get("key")
					   .isNull());
		assertEquals("ok", node.get("visible")
								.asString());
	}

	@Test
	void toJsonHandlesArrayNodeWithObscuring() throws Exception {
		// Array of objects whose fields should be masked (covers node.isArray() branch)
		record Row(String secret, String name) {
		}

		var rows = List.of(new Row("hideMe", "Alice"), new Row("hideToo", "Bob"));
		var jsonString = JsonTools.toJson(rows, List.of("secret"));
		JsonNode arr = new ObjectMapper().readTree(jsonString);
		assertTrue(arr.isArray());
		assertEquals(2, arr.size());
		assertEquals("hi*Me", arr.get(0)
								 .get("secret")
								 .asString());
		assertEquals("hi*oo", arr.get(1)
								 .get("secret")
								 .asString());
	}

	@Test
	void toJsonHandlesListOfMapsInField() throws Exception {
		// List whose items are Maps that contain a sensitive field
		// covers: item instanceof Map<?,?> nestedMap branch in maskMap
		Map<String, Object> parent = new java.util.LinkedHashMap<>();
		List<Map<String, Object>> children = List.of(
				Map.of("token", "abc123", "id", "1"),
				Map.of("token", "xyz789", "id", "2"));
		parent.put("children", children);
		parent.put("name", "parent");

		var jsonString = JsonTools.toJson(parent, List.of("token"));
		JsonNode node = new ObjectMapper().readTree(jsonString);
		// tokens should be masked
		assertEquals("ab*23", node.get("children")
								  .get(0)
								  .get("token")
								  .asString());
		assertEquals("xy*89", node.get("children")
								  .get(1)
								  .get("token")
								  .asString());
	}

	@Test
	void toJsonShortValueGetsFullyMasked() throws Exception {
		// "1234" → 4 chars → all masked as "****"
		Map<String, String> payload = Map.of("pin", "1234");
		var jsonString = JsonTools.toJson(payload, List.of("pin"));
		JsonNode node = new ObjectMapper().readTree(jsonString);
		assertEquals("****", node.get("pin")
								 .asString());
	}

	@Test
	void toJsonNoObscureFieldsDoesNotMask() throws Exception {
		Map<String, String> payload = Map.of("key", "value");
		var jsonString = JsonTools.toJson(payload);
		JsonNode node = new ObjectMapper().readTree(jsonString);
		assertEquals("value", node.get("key")
								  .asString());
	}

	@Test
	void toJsonWithEmptyObscureListDoesNotMask() throws Exception {
		Map<String, String> payload = Map.of("key", "value");
		var jsonString = JsonTools.toJson(payload, List.of());
		JsonNode node = new ObjectMapper().readTree(jsonString);
		assertEquals("value", node.get("key")
								  .asString());
	}

	@Test
	void toJsonHandlesListWithNonMapItems() throws Exception {
		// List of simple strings inside a field (covers list branch where items are NOT maps)
		Map<String, Object> parent = Map.of("tags", List.of("alpha", "beta", "gamma"),
				"secret", "passw0rd");
		var jsonString = JsonTools.toJson(parent, List.of("secret"));
		JsonNode node = new ObjectMapper().readTree(jsonString);
		assertEquals("pa*rd", node.get("secret")
								  .asString());
		assertTrue(node.get("tags")
					   .isArray());
	}

	@Test
	void toJsonHandlesComplexStructureAndObscuresEntries() throws Exception {
		record Complex(String label, String credential, Instant timeStamp, Map<String, String> metadata, List<String> tags) {
		}

		var container = new Complex("demo",
									"adminPass",
									Instant.now()
										   .minus(30, ChronoUnit.HOURS),
									Map.of("configSecret", "important", "normalKey", "ok"),
									List.of("alpha", "beta"));
		var jsonString = JsonTools.toJson(container, List.of("credential", "configSecret"));
		log.info("Masked Complex JSON: {}", jsonString);
		JsonNode result = new ObjectMapper().readTree(jsonString);

		assertEquals("demo", result.get("label")
								   .asString());
		assertEquals("ad*ss", result.get("credential")
									.asString());

		JsonNode metadata = result.get("metadata");
		assertEquals("im*nt", metadata.get("configSecret")
									  .asString());
		assertEquals("ok", metadata.get("normalKey")
								   .asString());

		JsonNode tags = result.get("tags");
		assertTrue(tags.isArray());
		assertEquals(2, tags.size());
		assertEquals("alpha", tags.get(0)
								  .asString());
		assertEquals("beta", tags.get(1)
								 .asString());
	}
}
