package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ConfigCenterObjectMapper {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private ConfigCenterObjectMapper() {
	}

	public static ObjectMapper create() {
		return OBJECT_MAPPER.copy();
	}
}
