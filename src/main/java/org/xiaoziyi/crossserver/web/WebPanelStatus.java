package org.xiaoziyi.crossserver.web;

import java.util.List;
import java.util.Map;

public record WebPanelStatus(
		String serverId,
		String cluster,
		int namespaceCount,
		int localSessionCount,
		int remoteSessionCount,
		int preparedTransferCount,
		String teleportGatewayType,
		int handoffSeconds,
		boolean redisEnabled,
		int pendingInvalidationCount,
		List<Map<String, Object>> nodes
) {
}
