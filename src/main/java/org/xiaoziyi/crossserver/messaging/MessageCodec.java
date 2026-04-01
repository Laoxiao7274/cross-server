package org.xiaoziyi.crossserver.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class MessageCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	public String encode(SyncMessage message) {
		try {
			return OBJECT_MAPPER.writeValueAsString(message);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化同步消息", exception);
		}
	}

	public SyncMessage decode(String value) {
		try {
			return OBJECT_MAPPER.readValue(value, SyncMessage.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化同步消息", exception);
		}
	}
}
