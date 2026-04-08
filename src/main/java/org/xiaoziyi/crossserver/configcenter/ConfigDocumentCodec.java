package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;

import java.time.Instant;
import java.util.Optional;

public final class ConfigDocumentCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private ConfigDocumentCodec() {
	}

	public static ConfigDocument fromSnapshot(GlobalSnapshot snapshot) {
		return fromSnapshot(snapshot.namespace(), snapshot.dataKey(), snapshot.payload(), snapshot.version(), snapshot.updatedAt());
	}

	public static ConfigDocument fromSnapshot(String namespace, String dataKey, String payload, long version, Instant updatedAt) {
		try {
			JsonNode root = OBJECT_MAPPER.readTree(payload);
			return new ConfigDocument(
					namespace,
					dataKey,
					payload,
					version,
					root.path("schemaVersion").asInt(1),
					readNullableText(root, "updatedBy"),
					readInstant(root, "updatedAt").orElse(updatedAt),
					readNullableText(root, "source"),
					readNullableText(root, "summary")
			);
		} catch (Exception exception) {
			throw new IllegalStateException("无法解析配置中心文档元信息: " + namespace + "/" + dataKey, exception);
		}
	}

	private static String readNullableText(JsonNode root, String field) {
		JsonNode node = root.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		String text = node.asText();
		return text == null || text.isBlank() ? null : text;
	}

	private static Optional<Instant> readInstant(JsonNode root, String field) {
		String text = readNullableText(root, field);
		if (text == null) {
			return Optional.empty();
		}
		return Optional.of(Instant.parse(text));
	}
}
