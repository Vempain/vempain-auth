package fi.poltsi.vempain.auth.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.experimental.UtilityClass;

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
			objectNode.fieldNames()
					  .forEachRemaining(fieldName -> {
						  JsonNode child = objectNode.get(fieldName);
						  if (targetField.equals(fieldName) && !child.isNull()) {
							  objectNode.set(fieldName, maskedNode(child));
						  }
						  maskFields(child, targetField);
					  });
		} else if (node.isArray()) {
			for (JsonNode child : node) {
				maskFields(child, targetField);
			}
		}
	}

	private static JsonNode maskedNode(JsonNode original) {
		if (original.isNull()) {
			return original;
		}
		String value  = original.isTextual() ? original.textValue() : original.asText();
		String masked = maskValue(value);
		return TextNode.valueOf(masked);
	}

	private static String maskValue(String value) {
		if (value == null || value.length() <= 4) {
			return value;
		}

		int len = value.length();
		return value.substring(0, 2) + "*" + value.substring(len - 2);
	}
}
