package org.xiaoziyi.crossserver.config;

import java.time.Instant;
import java.util.Map;

public record RouteTableSnapshot(
		int schemaVersion,
		Map<String, String> routes,
		String updatedBy,
		Instant updatedAt
) {
}
