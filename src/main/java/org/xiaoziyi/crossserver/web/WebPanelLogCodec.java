package org.xiaoziyi.crossserver.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class WebPanelLogCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private WebPanelLogCodec() {
	}

	public static String encode(WebPanelLogSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化 Web 面板日志快照", exception);
		}
	}

	public static WebPanelLogSnapshot decode(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, WebPanelLogSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化 Web 面板日志快照", exception);
		}
	}
}
