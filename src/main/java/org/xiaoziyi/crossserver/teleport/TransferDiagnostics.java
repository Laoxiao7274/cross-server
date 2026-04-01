package org.xiaoziyi.crossserver.teleport;

import org.xiaoziyi.crossserver.session.SessionTransferState;

import java.util.List;
import java.util.UUID;

public record TransferDiagnostics(
		UUID playerId,
		String playerName,
		TeleportHandoff handoff,
		SessionTransferState sessionTransferState,
		boolean localPreparedTransfer,
		TransferRecoveryStatus recoveryStatus,
		List<String> suggestedActions,
		List<TransferHistoryEntry> recentHistory
) {
	public boolean hasAnyState() {
		return handoff != null || localPreparedTransfer || (sessionTransferState != null && sessionTransferState.hasPreparedTransfer());
	}
}
