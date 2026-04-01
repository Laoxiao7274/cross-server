package org.xiaoziyi.crossserver.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class AuthCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private AuthCodec() {
	}

	public static String encodeProfile(AuthProfile profile) {
		try {
			return OBJECT_MAPPER.writeValueAsString(profile);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化登录资料", exception);
		}
	}

	public static AuthProfile decodeProfile(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, AuthProfile.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化登录资料", exception);
		}
	}

	public static String encodeTicket(AuthTicket ticket) {
		try {
			return OBJECT_MAPPER.writeValueAsString(ticket);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化登录票据", exception);
		}
	}

	public static AuthTicket decodeTicket(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, AuthTicket.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化登录票据", exception);
		}
	}
}
