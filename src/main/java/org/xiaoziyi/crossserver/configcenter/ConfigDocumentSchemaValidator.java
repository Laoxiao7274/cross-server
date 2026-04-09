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
		for (String path : schema.nonEmptyPaths()) {
			JsonNode node = resolve(root, path);
			if (node == null || node.isMissingNode() || node.isNull()) {
				continue;
			}
			if ((node.isTextual() && node.asText().trim().isEmpty()) || (node.isArray() && node.isEmpty()) || (node.isObject() && node.isEmpty())) {
				errors.add("字段不能为空: " + path);
			}
		}
		for (Map.Entry<String, List<String>> entry : schema.enumValues().entrySet()) {
			JsonNode node = resolve(root, entry.getKey());
			if (node == null || node.isMissingNode() || node.isNull()) {
				continue;
			}
			String value = node.isTextual() ? node.asText() : node.toString();
			if (!entry.getValue().contains(value)) {
				errors.add("字段取值非法: " + entry.getKey() + " 允许值为 " + String.join(", ", entry.getValue()));
			}
		}
		for (Map.Entry<String, Double> entry : schema.minValues().entrySet()) {
			JsonNode node = resolve(root, entry.getKey());
			if (node == null || node.isMissingNode() || node.isNull() || !node.isNumber()) {
				continue;
			}
			if (node.asDouble() < entry.getValue()) {
				errors.add("字段值过小: " + entry.getKey() + " 最小值为 " + entry.getValue());
			}
		}
		for (Map.Entry<String, Double> entry : schema.maxValues().entrySet()) {
			JsonNode node = resolve(root, entry.getKey());
			if (node == null || node.isMissingNode() || node.isNull() || !node.isNumber()) {
				continue;
			}
			if (node.asDouble() > entry.getValue()) {
				errors.add("字段值过大: " + entry.getKey() + " 最大值为 " + entry.getValue());
			}
		}
		for (Map.Entry<String, String> entry : schema.arrayItemTypes().entrySet()) {
			JsonNode node = resolve(root, entry.getKey());
			if (node == null || node.isMissingNode() || node.isNull() || !node.isArray()) {
				continue;
			}
			for (int i = 0; i < node.size(); i++) {
				if (!matchesType(node.get(i), entry.getValue())) {
					errors.add("数组元素类型不匹配: " + entry.getKey() + "[" + i + "] 需要 " + entry.getValue());
				}
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
