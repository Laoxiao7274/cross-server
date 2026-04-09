package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ConfigDocumentFormats {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private ConfigDocumentFormats() {
	}

	public static ConfigDocumentFormat detect(String payload) {
		String text = payload == null ? "" : payload.trim();
		if (text.isEmpty()) {
			return ConfigDocumentFormat.JSON;
		}
		try {
			JsonNode node = JSON_MAPPER.readTree(text);
			if (node != null) {
				return ConfigDocumentFormat.JSON;
			}
		} catch (Exception ignored) {
		}
		try {
			JsonNode node = YAML_MAPPER.readTree(text);
			if (node != null) {
				return ConfigDocumentFormat.YAML;
			}
		} catch (Exception ignored) {
		}
		throw new IllegalArgumentException("payload 必须是合法 JSON 或 YAML");
	}

	public static JsonNode parse(String payload) {
		ConfigDocumentFormat format = detect(payload);
		try {
			return mapper(format).readTree(payload);
		} catch (Exception exception) {
			throw new IllegalArgumentException("无法解析配置文档: " + exception.getMessage(), exception);
		}
	}

	public static String pretty(String payload) {
		if (payload == null || payload.isBlank()) {
			return "";
		}
		ConfigDocumentFormat format = detect(payload);
		try {
			JsonNode node = mapper(format).readTree(payload);
			return mapper(format).writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (Exception exception) {
			return payload;
		}
	}

	public static String write(JsonNode node, ConfigDocumentFormat format) {
		try {
			return mapper(format).writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (Exception exception) {
			throw new IllegalStateException("无法写出配置文档", exception);
		}
	}

	private static ObjectMapper mapper(ConfigDocumentFormat format) {
		return format == ConfigDocumentFormat.YAML ? YAML_MAPPER : JSON_MAPPER;
	}
}
