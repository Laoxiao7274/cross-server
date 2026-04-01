package org.xiaoziyi.crossserver.teleport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

public final class TeleportRequestCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private TeleportRequestCodec() {
	}

	public static String encode(Map<String, TeleportRequestService.PendingRequest> requests) {
		try {
			return OBJECT_MAPPER.writeValueAsString(requests);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化 TPA 请求", exception);
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, TeleportRequestService.PendingRequest> decode(String payload) {
		try {
			return OBJECT_MAPPER.readValue(
					payload,
					OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, TeleportRequestService.PendingRequest.class)
			);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化 TPA 请求", exception);
		}
	}
}
