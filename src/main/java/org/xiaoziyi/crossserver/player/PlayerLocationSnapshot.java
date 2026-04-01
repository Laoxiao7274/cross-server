package org.xiaoziyi.crossserver.player;

import java.time.Instant;

public record PlayerLocationSnapshot(
		String serverId,
		String world,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		boolean online,
		Instant updatedAt
) {
}
