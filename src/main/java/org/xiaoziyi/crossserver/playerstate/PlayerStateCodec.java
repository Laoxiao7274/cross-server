package org.xiaoziyi.crossserver.playerstate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
			JsonNode node = OBJECT_MAPPER.readTree(payload);
			return new PlayerStateSnapshot(
					node.path("health").asDouble(20.0D),
					node.path("maxHealth").asDouble(20.0D),
					node.path("foodLevel").asInt(20),
					(float) node.path("saturation").asDouble(5.0D),
					(float) node.path("exhaustion").asDouble(0.0D),
					node.path("level").asInt(0),
					(float) node.path("exp").asDouble(0.0D),
					node.path("totalExperience").asInt(0),
					node.path("fireTicks").asInt(0),
					node.path("remainingAir").asInt(300),
					node.path("gameMode").asText(null)
			);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化玩家状态", exception);
		}
	}
}
