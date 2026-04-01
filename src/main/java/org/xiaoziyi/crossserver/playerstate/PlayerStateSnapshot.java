package org.xiaoziyi.crossserver.playerstate;

public record PlayerStateSnapshot(
		double health,
		double maxHealth,
		int foodLevel,
		float saturation,
		float exhaustion,
		int level,
		float exp,
		int totalExperience,
		int fireTicks,
		int remainingAir
) {
}
