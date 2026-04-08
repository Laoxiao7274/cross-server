package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;

public final class ConfigDocumentPayloadFactory {
	private static final ObjectMapper OBJECT_MAPPER = ConfigCenterObjectMapper.create();

	private ConfigDocumentPayloadFactory() {
	}

	public static String enrich(String payload, int schemaVersion, String updatedBy, String source, String summary) {
		try {
			JsonNode parsed = OBJECT_MAPPER.readTree(payload);
			if (!(parsed instanceof com.fasterxml.jackson.databind.node.ObjectNode root)) {
				throw new IllegalStateException("配置中心文档必须是 JSON 对象");
			}
			root.put("schemaVersion", schemaVersion <= 0 ? 1 : schemaVersion);
			writeNullable(root, "updatedBy", updatedBy);
			root.put("updatedAt", Instant.now().toString());
			writeNullable(root, "source", source);
			writeNullable(root, "summary", summary);
			return OBJECT_MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法构建配置中心文档", exception);
		}
	}

	public static Optional<String> readText(String payload, String field) {
		try {
			JsonNode root = OBJECT_MAPPER.readTree(payload);
			JsonNode node = root.get(field);
			if (node == null || node.isNull()) {
				return Optional.empty();
			}
			String text = node.asText();
			return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法读取配置中心文档字段: " + field, exception);
		}
	}

	private static void writeNullable(com.fasterxml.jackson.databind.node.ObjectNode root, String field, String value) {
		if (value == null || value.isBlank()) {
			root.putNull(field);
			return;
		}
		root.put(field, value.trim());
	}
}
