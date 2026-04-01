package org.xiaoziyi.crossserver.messaging;

import java.time.Instant;

public record SyncMessage(
		String sourceServerId,
		String targetType,
		String targetId,
		String namespace,
		String action,
		long version,
		Instant occurredAt
) {
}
