package org.xiaoziyi.crossserver.teleport;

import java.time.Instant;
import java.util.UUID;

public record TeleportHandoff(
		String requestId,
		UUID playerId,
		Instant createdAt,
		Instant expiresAt,
		String sourceServerId,
		String targetServerId,
		String targetWorld,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		TeleportCause cause,
		String causeRef,
		String sessionTransferToken,
		TeleportHandoffStatus status,
		Instant gatewaySentAt,
		Instant consumedAt,
		String consumedServerId,
		Instant ackedAt,
		String ackedByServerId,
		String rollbackRequestId,
		String rollbackState,
		Instant rolledBackAt,
		String recoveryState,
		String preparedTransferState,
		Instant preparedTransferClearedAt,
		Instant lastUpdatedAt,
		String failureReason
) {
}
