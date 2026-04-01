package org.xiaoziyi.crossserver.homes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class HomesCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private HomesCodec() {
	}

	public static String encode(HomesSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化家园数据", exception);
		}
	}

	public static HomesSnapshot decode(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, HomesSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化家园数据", exception);
		}
	}
}
