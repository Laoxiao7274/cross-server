package org.xiaoziyi.crossserver.model;

import java.time.Instant;

public record NodeStatusRecord(
		String serverId,
		String cluster,
		String status,
		long latencyMillis,
		Instant lastSeen,
		Instant updatedAt
) {
}
