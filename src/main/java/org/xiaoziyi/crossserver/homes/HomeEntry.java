package org.xiaoziyi.crossserver.homes;

import java.time.Instant;

public record HomeEntry(
		String name,
		String serverId,
		String world,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		Instant createdAt,
		Instant updatedAt
) {
}
