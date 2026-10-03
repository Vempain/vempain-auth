package fi.poltsi.vempain.auth.tools;

import lombok.experimental.UtilityClass;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@UtilityClass
public class JsonTools {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static String toJson(Object source) {
		return toJson(source, List.of());
	}

	public static String toJson(Object source, List<String> obscureFields) {
		try {
			JsonNode root = MAPPER.valueToTree(source);
			if (obscureFields != null && !obscureFields.isEmpty()) {
				obscureFields.forEach(field -> maskFields(root, field));
			}
			return MAPPER.writeValueAsString(root);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to serialize object to JSON", e);
		}
	}

	private static void maskFields(JsonNode node, String targetField) {
		if (node == null || targetField == null || targetField.isBlank()) {
			return;
		}
		if (node.isObject()) {
			ObjectNode objectNode = (ObjectNode) node;
			for (var entry : objectNode.properties()) {
				if (targetField.equals(entry.getKey())) {
					JsonNode value = entry.getValue();
					if (value != null && !value.isNull()) {
						String rawValue = value.isString() ? value.asString() : value.toString();
						objectNode.put(entry.getKey(), maskValue(rawValue));
					}
				} else {
					maskFields(entry.getValue(), targetField);
				}
			}
		} else if (node.isArray()) {
			for (JsonNode child : node) {
				maskFields(child, targetField);
			}
		}
	}


	private static String maskValue(String value) {
		if (value == null) {
			return value;
		}
		int len = value.length();
		if (len <= 4) {
			// Mask all characters for short values
			return "*".repeat(len);
		}
		return value.substring(0, 2) + "*" + value.substring(len - 2);
	}
}
