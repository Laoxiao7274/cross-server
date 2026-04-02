package org.xiaoziyi.crossserver.config;

import java.time.Instant;

public record SharedModuleConfigSnapshot(
		int schemaVersion,
		Boolean auth,
		Boolean homes,
		Boolean warps,
		Boolean tpa,
		Boolean routeConfig,
		Boolean transferAdmin,
		Boolean economyBridge,
		Boolean permissions,
		String updatedBy,
		Instant updatedAt
) {
}
