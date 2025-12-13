package fi.poltsi.vempain.auth.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

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
										.asText());
		assertEquals(30, simpleNode.get("age")
								   .asInt());

		Map<String, String> payload    = Map.of("secret", "topsecret", "visible", "ok");
		var                 jsonString = JsonTools.toJson(payload, List.of("secret"));
		log.info("Masked JSON: {}", jsonString);
		JsonNode masked = new ObjectMapper().readTree(jsonString);
		assertEquals("to*et", masked.get("secret")
									.asText());
		assertEquals("ok", masked.get("visible")
								 .asText());
	}

	@Test
	void toJsonHandlesComplexStructureAndObscuresEntries() throws Exception {
		record Complex(String label, String credential, Map<String, String> metadata, List<String> tags) {
		}

		var container  = new Complex("demo", "adminPass", Map.of("configSecret", "important", "normalKey", "ok"), List.of("alpha", "beta"));
		var jsonString = JsonTools.toJson(container, List.of("credential", "configSecret"));
		log.info("Masked Complex JSON: {}", jsonString);
		JsonNode result = new ObjectMapper().readTree(jsonString);

		assertEquals("demo", result.get("label")
								   .asText());
		assertEquals("ad*ss", result.get("credential")
									.asText());

		JsonNode metadata = result.get("metadata");
		assertEquals("im*nt", metadata.get("configSecret")
									  .asText());
		assertEquals("ok", metadata.get("normalKey")
								   .asText());

		JsonNode tags = result.get("tags");
		assertTrue(tags.isArray());
		assertEquals(2, tags.size());
		assertEquals("alpha", tags.get(0)
								  .asText());
		assertEquals("beta", tags.get(1)
								 .asText());
	}
}
