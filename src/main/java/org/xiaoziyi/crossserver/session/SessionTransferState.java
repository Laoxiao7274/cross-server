package org.xiaoziyi.crossserver.session;

import java.time.Instant;

public record SessionTransferState(
		String currentServer,
		String sessionId,
		Instant lockUntil,
		String transferToken,
		String transferTargetServer,
		Instant transferPreparedAt,
		Instant transferExpiresAt
) {
	public boolean hasPreparedTransfer() {
		return transferToken != null && !transferToken.isBlank() && transferExpiresAt != null;
	}
}
