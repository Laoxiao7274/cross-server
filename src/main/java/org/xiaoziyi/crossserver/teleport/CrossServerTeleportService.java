package org.xiaoziyi.crossserver.teleport;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.auth.AuthService;
import org.xiaoziyi.crossserver.homes.HomesSyncService;
import org.xiaoziyi.crossserver.inventory.PlayerInventorySyncService;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.playerstate.PlayerStateSyncService;
import org.xiaoziyi.crossserver.session.SessionTransferState;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class CrossServerTeleportService {
	public static final String NAMESPACE = "teleport.handoff";
	public static final String ROLLBACK_INVENTORY_NAMESPACE = "teleport.rollback.inventory";
	public static final String ROLLBACK_ENDER_CHEST_NAMESPACE = "teleport.rollback.enderchest";
	public static final String ROLLBACK_PLAYER_STATE_NAMESPACE = "teleport.rollback.state";
	private static final Duration ACK_STALE_AFTER = Duration.ofSeconds(15);
	private static final Duration PREPARING_STALE_AFTER = Duration.ofSeconds(15);

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;
	private final AuthService authService;
	private final PlayerInventorySyncService inventorySyncService;
	private final PlayerStateSyncService playerStateSyncService;
	private final StorageProvider storageProvider;
	private HomesSyncService homesSyncService;
	private final ServerTransferGateway transferGateway;
	private final String serverId;
	private final Duration handoffTtl;
	private final int cooldownSeconds;
	private final Map<UUID, Instant> lastTeleportTime = new ConcurrentHashMap<>();

	public CrossServerTeleportService(
			JavaPlugin plugin,
			Logger logger,
			CrossServerApi api,
			AuthService authService,
			PlayerInventorySyncService inventorySyncService,
			PlayerStateSyncService playerStateSyncService,
			StorageProvider storageProvider,
			HomesSyncService homesSyncService,
			ServerTransferGateway transferGateway,
			String serverId,
			Duration handoffTtl,
			int cooldownSeconds
	) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
		this.authService = authService;
		this.inventorySyncService = inventorySyncService;
		this.playerStateSyncService = playerStateSyncService;
		this.storageProvider = storageProvider;
		this.homesSyncService = homesSyncService;
		this.transferGateway = transferGateway;
		this.serverId = serverId;
		this.handoffTtl = handoffTtl;
		this.cooldownSeconds = cooldownSeconds;
	}

	public void bindHomesSyncService(HomesSyncService homesSyncService) {
		this.homesSyncService = homesSyncService;
	}

	public TeleportInitiationResult requestTeleport(Player player, TeleportTarget target, TeleportCause cause, String causeRef) {
		if (!player.isOnline()) {
			return new TeleportInitiationResult(false, false, "§c玩家当前不在线，无法准备跨服传送。", null);
		}
		if (target == null) {
			return new TeleportInitiationResult(false, false, "§c无效的跨服目标。", null);
		}
		if (serverId.equalsIgnoreCase(target.serverId())) {
			return new TeleportInitiationResult(false, false, "§c目标仍在当前服务器，无需走跨服 handoff。", null);
		}
		if (cooldownSeconds > 0) {
			Instant lastTime = lastTeleportTime.get(player.getUniqueId());
			if (lastTime != null) {
				long elapsedSeconds = Duration.between(lastTime, Instant.now()).getSeconds();
				if (elapsedSeconds < cooldownSeconds) {
					return new TeleportInitiationResult(false, false,
							"§e跨服传送冷却中，请等待 " + (cooldownSeconds - elapsedSeconds) + " 秒。", null);
				}
			}
		}
		String requestId = UUID.randomUUID().toString();
		Instant now = Instant.now();
		String playerName = player.getName();
		try {
			saveRollbackSnapshot(player, requestId);
			flushPlayerData(player);
			String transferToken = api.sessionService().prepareTransfer(player.getUniqueId(), target.serverId(), handoffTtl);
			Instant pendingAt = Instant.now();
			TeleportHandoff pending = new TeleportHandoff(
					requestId,
					player.getUniqueId(),
					now,
					now.plus(handoffTtl),
					serverId,
					target.serverId(),
					target.world(),
					target.x(),
					target.y(),
					target.z(),
					target.yaw(),
					target.pitch(),
					cause,
					causeRef,
					transferToken,
					TeleportHandoffStatus.PENDING,
					pendingAt,
					null,
					null,
					null,
					null,
					requestId,
					"PENDING",
					null,
					"NONE",
					"PREPARED",
					null,
					pendingAt,
					null
			);
			logger.info("跨服传送准备完成: requestId=" + requestId + " player=" + player.getUniqueId() + " source=" + serverId + " target=" + target.serverId() + " cause=" + cause);
			authService.issueCrossServerTicket(player.getUniqueId());
			player.sendTitle("§b正在跨服传送", "§7目标服务器: §f" + target.serverId(), 5, 40, 10);
			player.sendActionBar(Component.text("跨服传送请求已创建: " + requestId));
			player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7F, 1.2F);
			saveHandoffSync(player.getUniqueId(), playerName, pending, "created", "handoff prepared and pending before gateway send");
			TeleportInitiationResult result = transferGateway.transfer(player, pending);
			if (!result.success()) {
				api.sessionService().clearPreparedTransfer(player.getUniqueId());
				markFailed(player.getUniqueId(), playerName, pending, "gateway send failed: " + result.message(), true, "gateway_failed");
				player.sendTitle("§c跨服传送失败", "§7网关发送失败", 5, 50, 10);
				player.sendActionBar(Component.text(result.message()));
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
				logger.warning("跨服传送网关发送失败: requestId=" + requestId + " player=" + player.getUniqueId() + " -> " + result.message());
				return result;
			}
			saveHandoffSync(player.getUniqueId(), playerName, pending, "gateway_sent", result.message());
			lastTeleportTime.put(player.getUniqueId(), Instant.now());
			logger.info("跨服传送网关发送成功: requestId=" + requestId + " player=" + player.getUniqueId() + " target=" + target.serverId());
			player.sendActionBar(Component.text(result.message()));
			return result;
		} catch (Exception exception) {
			logger.warning("准备跨服传送 handoff 失败: requestId=" + requestId + " player=" + player.getUniqueId() + " -> " + exception.getMessage());
			api.sessionService().clearPreparedTransfer(player.getUniqueId());
			player.sendTitle("§c跨服传送失败", "§7准备阶段出错", 5, 50, 10);
			player.sendActionBar(Component.text("跨服传送准备失败，请稍后重试。"));
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
			Instant failedAt = Instant.now();
			TeleportHandoff failed = new TeleportHandoff(
					requestId,
					player.getUniqueId(),
					now,
					now.plus(handoffTtl),
					serverId,
					target.serverId(),
					target.world(),
					target.x(),
					target.y(),
					target.z(),
					target.yaw(),
					target.pitch(),
					cause,
					causeRef,
					null,
					TeleportHandoffStatus.FAILED,
					null,
					null,
					null,
					null,
					null,
					requestId,
					"PENDING",
					null,
					"NONE",
					"CLEARED",
					failedAt,
					failedAt,
					"prepare transfer failed: " + exception.getMessage()
			);
			markRollbackPending(player.getUniqueId(), playerName, failed, "prepare_failed", failed.failureReason());
			return new TeleportInitiationResult(false, false, "§c跨服传送准备失败，请稍后重试。", requestId);
		}
	}

	public void tryConsumeArrival(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<PlayerSnapshot> snapshot = api.loadPlayerData(player.getUniqueId(), NAMESPACE);
				if (snapshot.isEmpty()) {
					return;
				}
				TeleportHandoff handoff = TeleportCodec.decode(snapshot.get().payload());
				TeleportHandoff resolved = resolveHandoffState(handoff);
				if (resolved.status() == TeleportHandoffStatus.EXPIRED) {
					api.sessionService().clearPreparedTransfer(player.getUniqueId());
					resolved = rollbackIfNeeded(player.getUniqueId(), player.getName(), resolved, player, "expired");
					saveHandoff(player.getUniqueId(), player.getName(), resolved, "expired", resolved.failureReason());
					logger.warning("跨服传送 handoff 已过期: requestId=" + resolved.requestId() + " player=" + player.getUniqueId());
					return;
				}
				if (resolved.status() != TeleportHandoffStatus.PENDING || !serverId.equalsIgnoreCase(resolved.targetServerId())) {
					if (shouldRecoverAck(resolved, player.getUniqueId())) {
						recoverAckState(player.getUniqueId(), player.getName(), resolved);
					}
					return;
				}
				TeleportHandoff finalResolved = resolved;
				plugin.getServer().getScheduler().runTask(plugin, () -> applyArrival(player, finalResolved));
			} catch (Exception exception) {
				logger.warning("消费跨服传送 handoff 失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public void reconcilePendingTransfers() {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
				try {
					reconcilePlayerTransfer(player.getUniqueId(), player.getName());
				} catch (Exception exception) {
					logger.warning("恢复 transfer 状态失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
				}
			}
		});
	}

	public void reconcilePlayerTransfer(UUID playerId, String playerName) throws Exception {
		Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, NAMESPACE);
		if (snapshot.isEmpty()) {
			return;
		}
		TeleportHandoff handoff = TeleportCodec.decode(snapshot.get().payload());
		TeleportHandoff resolved = resolveHandoffState(handoff);
		if (resolved.status() == TeleportHandoffStatus.EXPIRED) {
			api.sessionService().clearPreparedTransfer(playerId);
			resolved = rollbackIfNeeded(playerId, playerName, resolved, Bukkit.getPlayer(playerId), "expired");
			saveHandoff(playerId, playerName, resolved, "expired", resolved.failureReason());
			return;
		}
		if (shouldRecoverAck(resolved, playerId)) {
			recoverAckState(playerId, playerName, resolved);
			return;
		}
		if (shouldClearPreparedResidue(resolved, playerId)) {
			repairPreparedResidue(playerId, playerName, resolved);
		}
	}

	public void protectOnShutdown() {
		for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
			try {
				Optional<PlayerSnapshot> snapshot = api.loadPlayerData(player.getUniqueId(), NAMESPACE);
				if (snapshot.isEmpty()) {
					continue;
				}
				TeleportHandoff handoff = TeleportCodec.decode(snapshot.get().payload());
				if (handoff.status() != TeleportHandoffStatus.PENDING && handoff.status() != TeleportHandoffStatus.PREPARING) {
					continue;
				}
				api.sessionService().clearPreparedTransfer(player.getUniqueId());
				markFailed(player.getUniqueId(), player.getName(), handoff, "plugin disabling", true, "plugin_shutdown");
			} catch (Exception exception) {
				logger.warning("插件关闭时保护跨服传送失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
			}
		}
	}

	public void recoverRollbackOnJoin(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<PlayerSnapshot> snapshot = api.loadPlayerData(player.getUniqueId(), NAMESPACE);
				if (snapshot.isEmpty()) {
					return;
				}
				TeleportHandoff handoff = TeleportCodec.decode(snapshot.get().payload());
				if (handoff.status() != TeleportHandoffStatus.FAILED && handoff.status() != TeleportHandoffStatus.EXPIRED && handoff.status() != TeleportHandoffStatus.CANCELLED) {
					return;
				}
				if (!"PENDING".equalsIgnoreCase(handoff.rollbackState())) {
					return;
				}
				rollbackIfNeeded(player.getUniqueId(), player.getName(), handoff, player, "join_recover");
			} catch (Exception exception) {
				logger.warning("登录时恢复回滚快照失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	private TeleportHandoff resolveHandoffState(TeleportHandoff handoff) {
		Instant now = Instant.now();
		if (handoff.status() == TeleportHandoffStatus.PREPARING && handoff.lastUpdatedAt() != null && handoff.lastUpdatedAt().isBefore(now.minus(PREPARING_STALE_AFTER))) {
			return transitionHandoff(
					handoff,
					TeleportHandoffStatus.FAILED,
					handoff.gatewaySentAt(),
					null,
					null,
					handoff.ackedAt(),
					handoff.ackedByServerId(),
					handoff.rollbackRequestId(),
					handoff.rollbackState(),
					handoff.rolledBackAt(),
					"PREPARING_TIMEOUT",
					"CLEARED",
					now,
					now,
					"handoff preparing timed out"
			);
		}
		if (handoff.status() == TeleportHandoffStatus.PENDING && handoff.expiresAt() != null && handoff.expiresAt().isBefore(now)) {
			return transitionHandoff(
					handoff,
					TeleportHandoffStatus.EXPIRED,
					handoff.gatewaySentAt(),
					handoff.consumedAt(),
					handoff.consumedServerId(),
					handoff.ackedAt(),
					handoff.ackedByServerId(),
					handoff.rollbackRequestId(),
					handoff.rollbackState(),
					handoff.rolledBackAt(),
					"EXPIRED",
					"EXPIRED",
					now,
					now,
					"handoff expired"
			);
		}
		return handoff;
	}

	private void applyArrival(Player player, TeleportHandoff handoff) {
		if (!player.isOnline()) {
			return;
		}
		World world = Bukkit.getWorld(handoff.targetWorld());
		if (world == null) {
			player.sendMessage("§c跨服传送目标世界不存在: " + handoff.targetWorld());
			player.sendTitle("§c跨服传送失败", "§7目标世界不存在", 5, 50, 10);
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
			api.sessionService().clearPreparedTransfer(player.getUniqueId());
			markFailed(player.getUniqueId(), player.getName(), handoff, "target world missing", true, "arrival_failed");
			return;
		}
		if (!player.teleport(new org.bukkit.Location(world, handoff.x(), handoff.y(), handoff.z(), handoff.yaw(), handoff.pitch()))) {
			api.sessionService().clearPreparedTransfer(player.getUniqueId());
			player.sendTitle("§c跨服传送失败", "§7落点应用失败", 5, 50, 10);
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
			markFailed(player.getUniqueId(), player.getName(), handoff, "arrival apply failed", true, "arrival_failed");
			return;
		}
		clearRollbackSnapshotsAsync(player.getUniqueId());
		player.sendMessage("§a已应用跨服传送落点。§7请求: §f" + handoff.requestId());
		player.sendTitle("§a跨服传送完成", "§7已到达目标落点", 5, 40, 10);
		player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.2F);
		logger.info("跨服传送已完成: requestId=" + handoff.requestId() + " player=" + player.getUniqueId() + " target=" + serverId);
		Instant now = Instant.now();
		TeleportHandoff consumed = transitionHandoff(
				handoff,
				TeleportHandoffStatus.CONSUMED,
				handoff.gatewaySentAt(),
				now,
				serverId,
				now,
				serverId,
				handoff.rollbackRequestId(),
				"CLEARED",
				now,
				"ACKED",
				"CLAIMED",
				null,
				now,
				null
		);
		api.sessionService().clearPreparedTransfer(player.getUniqueId());
		saveHandoff(player.getUniqueId(), player.getName(), consumed, "consumed", "arrival applied and acked");
	}

	private boolean shouldRecoverAck(TeleportHandoff handoff, UUID playerId) {
		if (handoff.status() != TeleportHandoffStatus.CONSUMED || handoff.consumedAt() == null) {
			return false;
		}
		if (handoff.ackedAt() == null && handoff.consumedAt().isBefore(Instant.now().minus(ACK_STALE_AFTER))) {
			return true;
		}
		return shouldClearPreparedResidue(handoff, playerId);
	}

	private boolean shouldClearPreparedResidue(TeleportHandoff handoff, UUID playerId) {
		if (handoff.status() != TeleportHandoffStatus.CONSUMED && handoff.status() != TeleportHandoffStatus.FAILED && handoff.status() != TeleportHandoffStatus.EXPIRED && handoff.status() != TeleportHandoffStatus.CANCELLED) {
			return false;
		}
		try {
			Optional<SessionTransferState> sessionTransferState = api.sessionService().getSessionTransferState(playerId);
			return sessionTransferState.map(SessionTransferState::hasPreparedTransfer).orElse(false);
		} catch (Exception exception) {
			logger.warning("检查 prepared transfer 残留失败: player=" + playerId + " -> " + exception.getMessage());
			return false;
		}
	}

	private void recoverAckState(UUID playerId, String playerName, TeleportHandoff handoff) {
		api.sessionService().clearPreparedTransfer(playerId);
		Instant now = Instant.now();
		TeleportHandoff recovered = transitionHandoff(
				handoff,
				TeleportHandoffStatus.CONSUMED,
				handoff.gatewaySentAt(),
				handoff.consumedAt(),
				handoff.consumedServerId(),
				now,
				serverId,
				handoff.rollbackRequestId(),
				handoff.rollbackState(),
				handoff.rolledBackAt(),
				"RECOVERED_ACK",
				"CLEARED",
				now,
				now,
				handoff.failureReason()
		);
		saveHandoff(playerId, playerName, recovered, "recovered", "ack recovered and prepared cleared");
	}

	private void repairPreparedResidue(UUID playerId, String playerName, TeleportHandoff handoff) {
		api.sessionService().clearPreparedTransfer(playerId);
		Instant now = Instant.now();
		TeleportHandoff repaired = transitionHandoff(
				handoff,
				handoff.status(),
				handoff.gatewaySentAt(),
				handoff.consumedAt(),
				handoff.consumedServerId(),
				handoff.ackedAt(),
				handoff.ackedByServerId(),
				handoff.rollbackRequestId(),
				handoff.rollbackState(),
				handoff.rolledBackAt(),
				"CLEANUP_REPAIRED",
				"CLEARED",
				now,
				now,
				handoff.failureReason()
		);
		saveHandoff(playerId, playerName, repaired, "cleanup_repaired", "prepared transfer residue cleared");
	}

	private void markFailed(UUID playerId, String playerName, TeleportHandoff handoff, String reason, boolean clearPreparedTransfer, String eventType) {
		Instant now = Instant.now();
		TeleportHandoff failed = transitionHandoff(
				handoff,
				TeleportHandoffStatus.FAILED,
				handoff.gatewaySentAt(),
				null,
				null,
				handoff.ackedAt(),
				handoff.ackedByServerId(),
				handoff.rollbackRequestId(),
				handoff.rollbackState(),
				handoff.rolledBackAt(),
				clearPreparedTransfer ? "FAILED_CLEARED" : handoff.recoveryState(),
				clearPreparedTransfer ? "CLEARED" : handoff.preparedTransferState(),
				clearPreparedTransfer ? now : handoff.preparedTransferClearedAt(),
				now,
				reason
		);
		logger.warning("跨服传送失败: requestId=" + handoff.requestId() + " player=" + playerId + " -> " + reason);
		markRollbackPending(playerId, playerName, failed, eventType, reason);
	}

	private void markRollbackPending(UUID playerId, String playerName, TeleportHandoff handoff, String eventType, String detail) {
		TeleportHandoff pendingRollback = transitionHandoff(
				handoff,
				handoff.status(),
				handoff.gatewaySentAt(),
				handoff.consumedAt(),
				handoff.consumedServerId(),
				handoff.ackedAt(),
				handoff.ackedByServerId(),
				handoff.rollbackRequestId(),
				"PENDING",
				null,
				handoff.recoveryState(),
				handoff.preparedTransferState(),
				handoff.preparedTransferClearedAt(),
				Instant.now(),
				handoff.failureReason()
		);
		Player onlinePlayer = Bukkit.getPlayer(playerId);
		TeleportHandoff finalHandoff = rollbackIfNeeded(playerId, playerName, pendingRollback, onlinePlayer, eventType);
		saveHandoff(playerId, playerName, finalHandoff, eventType, detail);
	}

	private TeleportHandoff rollbackIfNeeded(UUID playerId, String playerName, TeleportHandoff handoff, Player player, String eventType) {
		if (!"PENDING".equalsIgnoreCase(handoff.rollbackState())) {
			return handoff;
		}
		if (player == null || !player.isOnline()) {
			return handoff;
		}
		try {
			Optional<PlayerSnapshot> inventorySnapshot = api.loadPlayerData(playerId, ROLLBACK_INVENTORY_NAMESPACE);
			Optional<PlayerSnapshot> enderChestSnapshot = api.loadPlayerData(playerId, ROLLBACK_ENDER_CHEST_NAMESPACE);
			Optional<PlayerSnapshot> stateSnapshot = api.loadPlayerData(playerId, ROLLBACK_PLAYER_STATE_NAMESPACE);
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				inventorySyncService.applyPayloads(player,
						inventorySnapshot.map(PlayerSnapshot::payload).orElse(null),
						enderChestSnapshot.map(PlayerSnapshot::payload).orElse(null));
				playerStateSyncService.applyPayload(player, stateSnapshot.map(PlayerSnapshot::payload).orElse(null));
				player.sendMessage("§e跨服传送失败，已自动恢复传送前的背包和状态。");
				player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.7F, 1.0F);
			});
			clearRollbackSnapshotsAsync(playerId);
			Instant now = Instant.now();
			return transitionHandoff(
					handoff,
					handoff.status(),
					handoff.gatewaySentAt(),
					handoff.consumedAt(),
					handoff.consumedServerId(),
					handoff.ackedAt(),
					handoff.ackedByServerId(),
					handoff.rollbackRequestId(),
					"APPLIED",
					now,
					handoff.recoveryState(),
					handoff.preparedTransferState(),
					handoff.preparedTransferClearedAt(),
					now,
					handoff.failureReason()
			);
		} catch (Exception exception) {
			logger.warning("回滚跨服传送快照失败: requestId=" + handoff.requestId() + " player=" + playerId + " event=" + eventType + " -> " + exception.getMessage());
			return handoff;
		}
	}

	private void saveRollbackSnapshot(Player player, String requestId) throws Exception {
		api.savePlayerData(player.getUniqueId(), ROLLBACK_INVENTORY_NAMESPACE, inventorySyncService.captureInventoryPayload(player));
		api.savePlayerData(player.getUniqueId(), ROLLBACK_ENDER_CHEST_NAMESPACE, inventorySyncService.captureEnderChestPayload(player));
		api.savePlayerData(player.getUniqueId(), ROLLBACK_PLAYER_STATE_NAMESPACE, playerStateSyncService.captureStatePayload(player));
		logger.info("已保存跨服回滚快照: requestId=" + requestId + " player=" + player.getUniqueId());
	}

	private void clearRollbackSnapshotsAsync(UUID playerId) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				api.savePlayerData(playerId, ROLLBACK_INVENTORY_NAMESPACE, "");
				api.savePlayerData(playerId, ROLLBACK_ENDER_CHEST_NAMESPACE, "");
				api.savePlayerData(playerId, ROLLBACK_PLAYER_STATE_NAMESPACE, "");
			} catch (Exception exception) {
				logger.warning("清理跨服回滚快照失败: player=" + playerId + " -> " + exception.getMessage());
			}
		});
	}

	private TeleportHandoff transitionHandoff(
			TeleportHandoff handoff,
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
		return new TeleportHandoff(
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
				status,
				gatewaySentAt,
				consumedAt,
				consumedServerId,
				ackedAt,
				ackedByServerId,
				rollbackRequestId,
				rollbackState,
				rolledBackAt,
				recoveryState,
				preparedTransferState,
				preparedTransferClearedAt,
				lastUpdatedAt,
				failureReason
		);
	}

	private void saveHandoffSync(UUID playerId, String playerName, TeleportHandoff handoff, String eventType, String detail) {
		try {
			api.savePlayerData(playerId, NAMESPACE, TeleportCodec.encode(handoff));
			storageProvider.appendTransferHistory(
					handoff.requestId(),
					playerId,
					playerName,
					handoff.sourceServerId(),
					handoff.targetServerId(),
					eventType,
					handoff.status(),
					detail
			);
		} catch (Exception exception) {
			logger.warning("保存跨服传送 handoff 失败: requestId=" + handoff.requestId() + " player=" + playerId + " -> " + exception.getMessage());
			throw new IllegalStateException("保存跨服传送 handoff 失败", exception);
		}
	}

	private void saveHandoff(UUID playerId, String playerName, TeleportHandoff handoff, String eventType, String detail) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				saveHandoffSync(playerId, playerName, handoff, eventType, detail);
			} catch (IllegalStateException ignored) {
				// 已在同步保存方法中记录日志
			}
		});
	}

	private void flushPlayerData(Player player) {
		inventorySyncService.savePlayerData(player);
		playerStateSyncService.savePlayerState(player);
		if (homesSyncService != null) {
			homesSyncService.savePlayerHomes(player);
		}
	}
}