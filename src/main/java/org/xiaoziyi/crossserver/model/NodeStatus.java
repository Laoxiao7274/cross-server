package org.xiaoziyi.crossserver.model;

import java.time.Instant;

public record NodeStatus(
		String serverId,
		String cluster,
		Instant lastSeen,
		Instant updatedAt
) {
}
