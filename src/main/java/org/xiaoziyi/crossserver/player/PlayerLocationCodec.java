package org.xiaoziyi.crossserver.player;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class PlayerLocationCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private PlayerLocationCodec() {
	}

	public static String encode(PlayerLocationSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化玩家位置", exception);
		}
	}

	public static PlayerLocationSnapshot decode(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, PlayerLocationSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化玩家位置", exception);
		}
	}
}
