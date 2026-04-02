package org.xiaoziyi.crossserver.permission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class PermissionSnapshotCodec {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private PermissionSnapshotCodec() {
	}

	public static String encode(PlayerPermissionSnapshot snapshot) {
		try {
			return OBJECT_MAPPER.writeValueAsString(snapshot);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法序列化玩家权限快照", exception);
		}
	}

	public static PlayerPermissionSnapshot decode(String payload) {
		try {
			JsonNode root = OBJECT_MAPPER.readTree(payload);
			JsonNode permissionsNode = root.path("permissions");
			List<String> permissions = new ArrayList<>();
			if (permissionsNode.isArray()) {
				for (JsonNode node : permissionsNode) {
					String permission = node.asText(null);
					if (permission != null && !permission.isBlank()) {
						permissions.add(permission);
					}
				}
			}
			return new PlayerPermissionSnapshot(List.copyOf(permissions));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("无法反序列化玩家权限快照", exception);
		}
	}
}
