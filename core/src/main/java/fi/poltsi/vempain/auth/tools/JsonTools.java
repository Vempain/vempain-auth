package fi.poltsi.vempain.auth.tools;

import lombok.experimental.UtilityClass;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.util.List;
import java.util.Map;

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
			Map<String, Object> asMap = MAPPER.convertValue(objectNode, new TypeReference<>() {
			});
			maskMap(asMap, targetField);
			ObjectNode replaced = MAPPER.valueToTree(asMap);
			objectNode.removeAll();
			objectNode.setAll(replaced);
		} else if (node.isArray()) {
			for (JsonNode child : node) {
				maskFields(child, targetField);
			}
		}
	}

	private static void maskMap(Map<String, Object> map, String targetField) {
		map.replaceAll((key, value) -> {
			if (value == null) {
				return null;
			}
			if (targetField.equals(key)) {
				return maskValue(value.toString());
			}
			if (value instanceof Map<?, ?> nested) {
				//noinspection unchecked
				maskMap((Map<String, Object>) nested, targetField);
			} else if (value instanceof List<?> list) {
				for (Object item : list) {
					if (item instanceof Map<?, ?> nestedMap) {
						//noinspection unchecked
						maskMap((Map<String, Object>) nestedMap, targetField);
					}
				}
			}
			return value;
		});
	}

	private static JsonNode maskedNode(JsonNode original) {
		if (original.isNull()) {
			return original;
		}
		String value  = original.isTextual() ? original.textValue() : original.asText();
		String masked = maskValue(value);
		return StringNode.valueOf(masked);
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
