package org.xiaoziyi.crossserver.web;

import java.time.Instant;
import java.util.Map;

public record WebPanelClusterSnapshot(
		int schemaVersion,
		String leaderServerId,
		Map<String, WebPanelMemberSnapshot> members,
		Instant updatedAt,
		String updatedBy,
		String source,
		String summary
) {
}
