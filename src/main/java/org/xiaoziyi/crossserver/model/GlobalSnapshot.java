package org.xiaoziyi.crossserver.model;

import java.time.Instant;

public record GlobalSnapshot(
		String namespace,
		String dataKey,
		String payload,
		long version,
		Instant updatedAt
) {
}
