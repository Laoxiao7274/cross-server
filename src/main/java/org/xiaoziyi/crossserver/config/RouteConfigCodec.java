package org.xiaoziyi.crossserver.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class RouteConfigCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private RouteConfigCodec() {
	}

	public static String encode(RouteTableSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化共享路由配置", exception);
		}
	}

	public static RouteTableSnapshot decode(String payload) {
		try {
			RouteTableSnapshot snapshot = OBJECT_MAPPER.readValue(payload, RouteTableSnapshot.class);
			return new RouteTableSnapshot(
					snapshot.schemaVersion(),
					snapshot.routes(),
					snapshot.updatedBy(),
					snapshot.updatedAt(),
					snapshot.source(),
					snapshot.summary()
			);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化共享路由配置", exception);
		}
	}
}
