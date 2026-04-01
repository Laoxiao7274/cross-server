package org.xiaoziyi.crossserver.teleport;

import java.time.Instant;
import java.util.UUID;

public record TransferHistoryEntry(
		long id,
		String requestId,
		UUID playerId,
		String playerName,
		String sourceServerId,
		String targetServerId,
		String eventType,
		TeleportHandoffStatus status,
		String detail,
		Instant createdAt
) {
}
