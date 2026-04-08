package org.xiaoziyi.crossserver.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class WebPanelClusterCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private WebPanelClusterCodec() {
	}

	public static String encode(WebPanelClusterSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化 Web 面板集群状态", exception);
		}
	}

	public static WebPanelClusterSnapshot decode(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, WebPanelClusterSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化 Web 面板集群状态", exception);
		}
	}
}
