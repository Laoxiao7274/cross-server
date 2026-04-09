package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Optional;

public final class ConfigDocumentPayloadFactory {
	private ConfigDocumentPayloadFactory() {
	}

	public static String enrich(String payload, int schemaVersion, String updatedBy, String source, String summary) {
		try {
			ConfigDocumentFormat format = ConfigDocumentFormats.detect(payload);
			JsonNode parsed = ConfigDocumentFormats.parse(payload);
			if (!(parsed instanceof ObjectNode root)) {
				throw new IllegalStateException("配置中心文档必须是 JSON 对象");
			}
			root.put("schemaVersion", schemaVersion <= 0 ? 1 : schemaVersion);
			writeNullable(root, "updatedBy", updatedBy);
			root.put("updatedAt", Instant.now().toString());
			writeNullable(root, "source", source);
			writeNullable(root, "summary", summary);
			return ConfigDocumentFormats.write(root, format);
		} catch (Exception exception) {
			throw new IllegalStateException("无法构建配置中心文档", exception);
		}
	}

	public static Optional<String> readText(String payload, String field) {
		try {
			JsonNode root = ConfigDocumentFormats.parse(payload);
			JsonNode node = root.get(field);
			if (node == null || node.isNull()) {
				return Optional.empty();
			}
			String text = node.asText();
			return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
		} catch (Exception exception) {
			throw new IllegalStateException("无法读取配置中心文档字段: " + field, exception);
		}
	}

	private static void writeNullable(ObjectNode root, String field, String value) {
		if (value == null || value.isBlank()) {
			root.putNull(field);
			return;
		}
		root.put(field, value.trim());
	}
}
