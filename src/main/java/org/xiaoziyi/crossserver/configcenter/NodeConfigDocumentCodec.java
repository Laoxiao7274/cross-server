package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class NodeConfigDocumentCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private NodeConfigDocumentCodec() {
	}

	public static String encodeSnapshot(NodeConfigSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化节点配置快照", exception);
		}
	}

	public static NodeConfigSnapshot decodeSnapshot(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, NodeConfigSnapshot.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化节点配置快照", exception);
		}
	}

	public static String encodeApplyRequest(NodeConfigApplyRequest request) {
		try {
			return OBJECT_MAPPER.writeValueAsString(request);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化节点配置申请", exception);
		}
	}

	public static NodeConfigApplyRequest decodeApplyRequest(String payload) {
		try {
			return OBJECT_MAPPER.readValue(payload, NodeConfigApplyRequest.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化节点配置申请", exception);
		}
	}
}
