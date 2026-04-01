package org.xiaoziyi.crossserver.teleport;

public record TeleportTarget(
		String serverId,
		String world,
		double x,
		double y,
		double z,
		float yaw,
		float pitch
) {
}
