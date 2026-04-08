package org.xiaoziyi.crossserver.configcenter;

import java.time.Instant;

public record ConfigChangeEvent(
		String namespace,
		String dataKey,
		long version,
		String sourceServerId,
		Instant occurredAt
) {
}
