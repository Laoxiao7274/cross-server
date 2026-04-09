package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConfigDocumentSchemaValidator {
	private ConfigDocumentSchemaValidator() {
	}

	public static void validate(String payload, ConfigDocumentSchema schema) {
		if (schema == null) {
			return;
		}
		JsonNode root = ConfigDocumentFormats.parse(payload);
		List<String> errors = new ArrayList<>();
		for (String requiredPath : schema.requiredPaths()) {
			JsonNode node = resolve(root, requiredPath);
			if (node == null || node.isMissingNode() || node.isNull()) {
				errors.add("缺少必填字段: " + requiredPath);
			}
		}
		for (Map.Entry<String, String> entry : schema.fieldTypes().entrySet()) {
			JsonNode node = resolve(root, entry.getKey());
			if (node == null || node.isMissingNode() || node.isNull()) {
				continue;
			}
			if (!matchesType(node, entry.getValue())) {
				errors.add("字段类型不匹配: " + entry.getKey() + " 需要 " + entry.getValue());
			}
		}
		if (!errors.isEmpty()) {
			throw new IllegalArgumentException(String.join("；", errors));
		}
	}

	private static JsonNode resolve(JsonNode root, String path) {
		JsonNode current = root;
		for (String segment : path.split("\\.")) {
			if (current == null) {
				return null;
			}
			current = current.path(segment);
		}
		return current;
	}

	private static boolean matchesType(JsonNode node, String expectedType) {
		return switch (String.valueOf(expectedType).toLowerCase()) {
			case "object" -> node.isObject();
			case "array" -> node.isArray();
			case "string" -> node.isTextual();
			case "boolean" -> node.isBoolean();
			case "number" -> node.isNumber();
			case "integer" -> node.isIntegralNumber();
			default -> true;
		};
	}
}
