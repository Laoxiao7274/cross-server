package org.xiaoziyi.crossserver.model;

import java.time.Instant;
import java.util.UUID;

public record PlayerSnapshot(
		UUID playerId,
		String namespace,
		String payload,
		long version,
		Instant updatedAt
) {
}
