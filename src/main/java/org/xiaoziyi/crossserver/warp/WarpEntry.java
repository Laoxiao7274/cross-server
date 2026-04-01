package org.xiaoziyi.crossserver.warp;

import java.time.Instant;

public record WarpEntry(
		String name,
		String serverId,
		String world,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		String createdBy,
		Instant createdAt,
		Instant updatedAt
) {
}
