package org.xiaoziyi.crossserver.teleport;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.session.SessionService;
import org.xiaoziyi.crossserver.session.SessionTransferState;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TransferAdminService {
	private static final int DEFAULT_PAGE_SIZE = 21;
	private static final int DEFAULT_HISTORY_SIZE = 5;
	private final CrossServerApi api;
	private final StorageProvider storageProvider;
	private final SessionService sessionService;

	public TransferAdminService(CrossServerApi api, StorageProvider storageProvider, SessionService sessionService) {
		this.api = api;
		this.storageProvider = storageProvider;
		this.sessionService = sessionService;
	}

	public Optional<TransferInspection> inspectPlayer(String playerName) throws Exception {
		Optional<UUID> playerId = storageProvider.findPlayerIdByName(playerName);
		if (playerId.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(inspectPlayer(playerId.get()));
	}

	public TransferInspection inspectPlayer(UUID playerId) throws Exception {
		TransferDiagnostics diagnostics = loadDiagnostics(playerId, DEFAULT_HISTORY_SIZE);
		return new TransferInspection(
				diagnostics.playerId(),
				diagnostics.playerName(),
				diagnostics.handoff(),
				diagnostics.localPreparedTransfer(),
				diagnostics.sessionTransferState(),
				diagnostics.recoveryStatus(),
				diagnostics.suggestedActions(),
				diagnostics.recentHistory()
		);
	}

	public TransferDiagnostics loadDiagnostics(UUID playerId, int historyLimit) throws Exception {
		String playerName = storageProvider.findPlayerName(playerId).orElse(playerId.toString());
		TeleportHandoff handoff = null;
		if (api != null) {
			Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, CrossServerTeleportService.NAMESPACE);
			handoff = snapshot.map(value -> TeleportCodec.decode(value.payload())).orElse(null);
		}
		SessionTransferState sessionTransferState = sessionService.getSessionTransferState(playerId).orElse(null);
		boolean localPreparedTransfer = sessionService.hasPreparedTransfer(playerId);
		List<TransferHistoryEntry> recentHistory = storageProvider.findTransferHistory(playerId, Math.max(1, Math.min(historyLimit, 20)));
		TransferRecoveryStatus recoveryStatus = deriveRecoveryStatus(handoff, sessionTransferState, localPreparedTransfer);
		return new TransferDiagnostics(
				playerId,
				playerName,
				handoff,
				sessionTransferState,
				localPreparedTransfer,
				recoveryStatus,
				buildSuggestedActions(handoff, sessionTransferState, localPreparedTransfer, recoveryStatus),
				recentHistory
		);
	}

	public List<TransferHistoryEntry> getTransferHistory(UUID playerId, int limit) throws Exception {
		return storageProvider.findTransferHistory(playerId, limit);
	}

	public TransferPageResult getTransferHistoryPage(UUID playerId, int page, int pageSize) throws Exception {
		int actualPageSize = clampPageSize(pageSize);
		int currentPage = Math.max(1, page);
		List<TransferHistoryEntry> allEntries = storageProvider.findTransferHistory(playerId, 100);
		return toPageResult(allEntries, currentPage, actualPageSize);
	}

	public List<TransferHistoryEntry> getRecentTransferHistory(int limit) throws Exception {
		return storageProvider.findRecentTransferHistory(limit);
	}

	public TransferPageResult getRecentTransferHistoryPage(int page, int pageSize) throws Exception {
		int actualPageSize = clampPageSize(pageSize);
		int currentPage = Math.max(1, page);
		List<TransferHistoryEntry> allEntries = storageProvider.findRecentTransferHistory(100);
		return toPageResult(allEntries, currentPage, actualPageSize);
	}

	public ClearResult clearTransfer(UUID playerId, String actorName) throws Exception {
		TransferDiagnostics diagnostics = loadDiagnostics(playerId, DEFAULT_HISTORY_SIZE);
		boolean localPreparedTransfer = diagnostics.localPreparedTransfer();
		boolean sessionPreparedTransfer = diagnostics.sessionTransferState() != null && diagnostics.sessionTransferState().hasPreparedTransfer();
		TeleportHandoff handoff = diagnostics.handoff();
		if (handoff == null && !localPreparedTransfer && !sessionPreparedTransfer) {
			return new ClearResult(false, "§e当前没有可清理的 transfer 状态。", inspectPlayer(playerId));
		}
		sessionService.clearPreparedTransfer(playerId);
		if (handoff != null && api != null) {
			Instant now = Instant.now();
			TeleportHandoff cleared = new TeleportHandoff(
					handoff.requestId(),
					handoff.playerId(),
					handoff.createdAt(),
					handoff.expiresAt(),
					handoff.sourceServerId(),
					handoff.targetServerId(),
					handoff.targetWorld(),
					handoff.x(),
					handoff.y(),
					handoff.z(),
					handoff.yaw(),
					handoff.pitch(),
					handoff.cause(),
					handoff.causeRef(),
					handoff.sessionTransferToken(),
					TeleportHandoffStatus.CANCELLED,
					handoff.gatewaySentAt(),
					handoff.consumedAt(),
					handoff.consumedServerId(),
					handoff.ackedAt(),
					handoff.ackedByServerId(),
					handoff.rollbackRequestId(),
					handoff.rollbackState(),
					handoff.rolledBackAt(),
					"ADMIN_CLEARED",
					"CLEARED",
					now,
					now,
					"cleared by admin: " + actorName
			);
			api.savePlayerData(playerId, CrossServerTeleportService.NAMESPACE, TeleportCodec.encode(cleared));
			storageProvider.appendTransferHistory(cleared.requestId(), playerId, diagnostics.playerName(), cleared.sourceServerId(), cleared.targetServerId(), "admin_clear", cleared.status(), cleared.failureReason());
		}
		return new ClearResult(true, "§a已清理 transfer 状态。", inspectPlayer(playerId));
	}

	private TransferPageResult toPageResult(List<TransferHistoryEntry> entries, int page, int pageSize) {
		int totalItems = entries.size();
		int totalPages = Math.max(1, (totalItems + pageSize - 1) / pageSize);
		int currentPage = Math.max(1, Math.min(page, totalPages));
		int fromIndex = Math.min(totalItems, (currentPage - 1) * pageSize);
		int toIndex = Math.min(totalItems, fromIndex + pageSize);
		return new TransferPageResult(currentPage, pageSize, totalItems, totalPages, entries.subList(fromIndex, toIndex));
	}

	private int clampPageSize(int pageSize) {
		return Math.max(1, Math.min(pageSize, 45));
	}

	private TransferRecoveryStatus deriveRecoveryStatus(TeleportHandoff handoff, SessionTransferState sessionTransferState, boolean localPreparedTransfer) {
		if (handoff == null) {
			if (sessionTransferState != null && sessionTransferState.hasPreparedTransfer()) {
				return TransferRecoveryStatus.EXPIRED_NEEDS_CLEANUP;
			}
			return localPreparedTransfer ? TransferRecoveryStatus.UNKNOWN : TransferRecoveryStatus.NONE;
		}
		return switch (handoff.status()) {
			case PREPARING -> TransferRecoveryStatus.PREPARING;
			case PENDING -> handoff.expiresAt() != null && handoff.expiresAt().isBefore(Instant.now())
					? TransferRecoveryStatus.EXPIRED_NEEDS_CLEANUP
					: TransferRecoveryStatus.IN_FLIGHT;
			case CONSUMED -> handoff.ackedAt() == null
					? TransferRecoveryStatus.ARRIVED_AWAITING_ACK
					: ("RECOVERED_ACK".equalsIgnoreCase(handoff.recoveryState()) ? TransferRecoveryStatus.RECOVERED : TransferRecoveryStatus.COMPLETED);
			case FAILED -> TransferRecoveryStatus.FAILED_NEEDS_CLEANUP;
			case EXPIRED -> TransferRecoveryStatus.EXPIRED_NEEDS_CLEANUP;
			case CANCELLED -> TransferRecoveryStatus.ADMIN_CLEARED;
		};
	}

	private List<String> buildSuggestedActions(TeleportHandoff handoff, SessionTransferState sessionTransferState, boolean localPreparedTransfer, TransferRecoveryStatus recoveryStatus) {
		List<String> actions = new ArrayList<>();
		switch (recoveryStatus) {
			case PREPARING, IN_FLIGHT -> actions.add("刷新观察状态推进");
			case ARRIVED_AWAITING_ACK -> actions.add("尝试恢复 ACK/清理残留 prepared");
			case EXPIRED_NEEDS_CLEANUP, FAILED_NEEDS_CLEANUP -> actions.add("执行清理并释放残留 transfer 状态");
			case ADMIN_CLEARED -> actions.add("当前已被管理员清理");
			case COMPLETED -> actions.add("当前已完成，通常无需处理");
			case RECOVERED -> actions.add("状态已恢复，建议确认目标服落点与会话");
			case UNKNOWN -> actions.add("存在局部残留，建议检查 prepared/baton 状态后清理");
			case NONE -> actions.add("当前没有可处理的 transfer 状态");
		}
		if (handoff != null && handoff.failureReason() != null && !handoff.failureReason().isBlank()) {
			actions.add("查看 failure reason: " + handoff.failureReason());
		}
		if (sessionTransferState != null && sessionTransferState.hasPreparedTransfer()) {
			actions.add("player_session 仍保留 transfer token");
		}
		if (localPreparedTransfer) {
			actions.add("当前节点仍有本地 prepared transfer 记录");
		}
		return actions;
	}

	public record TransferInspection(
			UUID playerId,
			String playerName,
			TeleportHandoff handoff,
			boolean localPreparedTransfer,
			SessionTransferState sessionTransferState,
			TransferRecoveryStatus recoveryStatus,
			List<String> suggestedActions,
			List<TransferHistoryEntry> recentHistory
	) {
	}

	public record ClearResult(boolean changed, String message, TransferInspection inspection) {
	}
}
