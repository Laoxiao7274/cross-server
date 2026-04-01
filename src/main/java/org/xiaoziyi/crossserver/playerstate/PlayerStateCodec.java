package org.xiaoziyi.crossserver.playerstate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class PlayerStateCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private PlayerStateCodec() {
	}

	public static String encode(PlayerStateSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化玩家状态", exception);
		}
	}

	public static PlayerStateSnapshot decode(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, PlayerStateSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化玩家状态", exception);
		}
	}
}
