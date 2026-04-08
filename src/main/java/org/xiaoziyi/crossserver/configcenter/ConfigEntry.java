package org.xiaoziyi.crossserver.configcenter;

import java.time.Instant;

public record ConfigEntry(
		String namespace,
		String dataKey,
		String payload,
		long version,
		Instant updatedAt
) {
}
