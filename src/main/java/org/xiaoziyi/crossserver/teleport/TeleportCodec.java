package org.xiaoziyi.crossserver.teleport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class TeleportCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private TeleportCodec() {
	}

	public static String encode(TeleportHandoff handoff) {
		try {
			return OBJECT_MAPPER.writeValueAsString(handoff);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化跨服传送 handoff", exception);
		}
	}

	public static TeleportHandoff decode(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, TeleportHandoff.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化跨服传送 handoff", exception);
		}
	}
}
