package org.xiaoziyi.crossserver.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;

public final class SharedModuleConfigCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private SharedModuleConfigCodec() {
	}

	public static String encode(SharedModuleConfigSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化共享模块开关配置", exception);
		}
	}

	public static SharedModuleConfigSnapshot decode(String payload) {
		try {
			JsonNode root = OBJECT_MAPPER.readTree(payload);
			return new SharedModuleConfigSnapshot(
					root.path("schemaVersion").asInt(1),
					readNullableBoolean(root, "auth"),
					readNullableBoolean(root, "homes"),
					readNullableBoolean(root, "warps"),
					readNullableBoolean(root, "tpa"),
					readNullableBoolean(root, "routeConfig"),
					readNullableBoolean(root, "transferAdmin"),
					readNullableBoolean(root, "economyBridge"),
					readNullableBoolean(root, "permissions"),
					readNullableText(root, "updatedBy"),
					readInstant(root, "updatedAt"),
					readNullableText(root, "source"),
					readNullableText(root, "summary")
			);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化共享模块开关配置", exception);
		}
	}

	private static Boolean readNullableBoolean(JsonNode root, String field) {
		JsonNode node = root.get(field);
		return node == null || node.isNull() ? null : node.asBoolean();
	}

	private static Instant readInstant(JsonNode root, String field) {
		JsonNode node = root.get(field);
		if (node == null || node.isNull() || node.asText().isBlank()) {
			return null;
		}
		return Instant.parse(node.asText());
	}

	private static String readNullableText(JsonNode root, String field) {
		JsonNode node = root.get(field);
		if (node == null || node.isNull()) {
			return null;
		}
		String value = node.asText();
		return value == null || value.isBlank() ? null : value;
	}
}
