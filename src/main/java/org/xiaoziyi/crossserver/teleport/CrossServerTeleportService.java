package org.xiaoziyi.crossserver.teleport;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.auth.AuthService;
import org.xiaoziyi.crossserver.homes.HomesSyncService;
import org.xiaoziyi.crossserver.inventory.PlayerInventorySyncService;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.permission.PlayerPermissionSyncService;
import org.xiaoziyi.crossserver.playerstate.PlayerStateSyncService;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Duration;
import java.time.Instant;
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
	private final PlayerPermissionSyncService playerPermissionSyncService;
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
			PlayerPermissionSyncService playerPermissionSyncService,
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
		this.playerPermissionSyncService = playerPermissionSyncService;
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
					return new TeleportInitiationResult(false, false, "§e跨服传送冷却中，请等待 " + (cooldownSeconds - elapsedSeconds) + " 秒。", null);
				}
			}
		}
		String requestId = UUID.randomUUID().toString();
		Instant now = Instant.now();
		String playerName = player.getName();
		try {
			saveRollbackSnapshot(player);
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
					null,
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
			if (authService != null) {
				authService.issueCrossServerTicket(player.getUniqueId());
			}
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
			TeleportHandoff gatewaySent = new TeleportHandoff(
					pending.requestId(),
					pending.playerId(),
					pending.createdAt(),
					pending.expiresAt(),
					pending.sourceServerId(),
					pending.targetServerId(),
					pending.targetWorld(),
					pending.x(),
					pending.y(),
					pending.z(),
					pending.yaw(),
					pending.pitch(),
					pending.cause(),
					pending.causeRef(),
					pending.sessionTransferToken(),
					pending.status(),
					Instant.now(),
					pending.consumedAt(),
					pending.consumedServerId(),
					pending.ackedAt(),
					pending.ackedByServerId(),
					pending.rollbackRequestId(),
					pending.rollbackState(),
					pending.rolledBackAt(),
					pending.recoveryState(),
					pending.preparedTransferState(),
					pending.preparedTransferClearedAt(),
					Instant.now(),
					pending.failureReason()
			);
			saveHandoffSync(player.getUniqueId(), playerName, gatewaySent, "gateway_sent", result.message());
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
			try {
				markRollbackPending(player.getUniqueId(), playerName, failed, "prepare_failed", failed.failureReason());
			} catch (Exception historyException) {
				logger.warning("记录跨服传送准备失败历史失败: requestId=" + requestId + " player=" + player.getUniqueId() + " -> " + historyException.getMessage());
			}
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
				if (handoff.status() != TeleportHandoffStatus.PENDING) {
					return;
				}
				if (!serverId.equalsIgnoreCase(handoff.targetServerId())) {
					return;
				}
				if (handoff.expiresAt().isBefore(Instant.now())) {
					markFailed(player.getUniqueId(), player.getName(), handoff, "target server consume expired handoff", false, "expired");
					plugin.getServer().getScheduler().runTask(plugin, () -> {
						player.sendTitle("§c跨服传送失败", "§7请求已过期", 5, 50, 10);
						player.sendActionBar(Component.text("跨服传送请求已过期"));
						player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
					});
					return;
				}
				World world = Bukkit.getWorld(handoff.targetWorld());
				if (world == null) {
					markFailed(player.getUniqueId(), player.getName(), handoff, "target world missing: " + handoff.targetWorld(), false, "world_missing");
					plugin.getServer().getScheduler().runTask(plugin, () -> {
						player.sendTitle("§c跨服传送失败", "§7目标世界不存在", 5, 50, 10);
						player.sendActionBar(Component.text("目标世界不存在: " + handoff.targetWorld()));
						player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
					});
					return;
				}
				plugin.getServer().getScheduler().runTask(plugin, () -> player.teleportAsync(new Location(world, handoff.x(), handoff.y(), handoff.z(), handoff.yaw(), handoff.pitch()))
						.thenAccept(success -> plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> completeArrival(player, handoff, success))));
			} catch (Exception exception) {
				logger.warning("消费跨服传送 handoff 失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	private void completeArrival(Player player, TeleportHandoff handoff, boolean success) {
		try {
			if (!success) {
				markFailed(player.getUniqueId(), player.getName(), handoff, "target server teleportAsync returned false", false, "teleport_failed");
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					player.sendTitle("§c跨服传送失败", "§7目标服务器传送失败", 5, 50, 10);
					player.sendActionBar(Component.text("目标服务器传送失败"));
					player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
				});
				return;
			}
			Instant arrivalAt = Instant.now();
			TeleportHandoff consumed = new TeleportHandoff(
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
					TeleportHandoffStatus.CONSUMED,
					handoff.gatewaySentAt(),
					arrivalAt,
					serverId,
					arrivalAt,
					serverId,
					handoff.rollbackRequestId(),
					"NONE",
					null,
					"ARRIVED",
					"CLEARED",
					arrivalAt,
					arrivalAt,
					null
			);
			saveHandoffSync(player.getUniqueId(), player.getName(), consumed, "consumed", "target server arrival confirmed");
			api.sessionService().clearPreparedTransfer(player.getUniqueId());
			clearRollbackSnapshots(player.getUniqueId());
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				player.sendTitle(successTitle(handoff), successSubtitle(handoff), 5, 45, 10);
				player.sendActionBar(Component.text(successActionBar(handoff)));
				player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.15F);
			});
		} catch (Exception exception) {
			logger.warning("完成跨服传送到达阶段失败: requestId=" + handoff.requestId() + " player=" + player.getUniqueId() + " -> " + exception.getMessage());
		}
	}

	private String successTitle(TeleportHandoff handoff) {
		return switch (handoff.cause()) {
			case HOME -> "§a家园传送成功";
			case WARP -> "§a地标传送成功";
			case TPA -> "§a玩家传送成功";
			case TPA_HERE -> "§a玩家传送成功";
		};
	}

	private String successSubtitle(TeleportHandoff handoff) {
		return switch (handoff.cause()) {
			case HOME -> "§7已到达家园所在节点 §f" + handoff.targetServerId();
			case WARP -> "§7已到达地标所在节点 §f" + handoff.targetServerId();
			case TPA -> "§7已到达目标玩家所在节点 §f" + handoff.targetServerId();
			case TPA_HERE -> "§7已完成玩家传送邀请 §f" + handoff.targetServerId();
		};
	}

	private String successActionBar(TeleportHandoff handoff) {
		return switch (handoff.cause()) {
			case HOME -> "已传送至目标家园";
			case WARP -> "已传送至目标地标";
			case TPA -> "已传送至目标玩家附近";
			case TPA_HERE -> "已完成玩家传送邀请";
		};
	}

	public void reconcilePendingTransfers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			try {
				reconcilePlayerTransfer(player.getUniqueId(), player.getName());
			} catch (Exception exception) {
				logger.warning("reconcile pending transfer 失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
			}
		}
	}

	public void reconcilePlayerTransfer(UUID playerId, String playerName) throws Exception {
		Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, NAMESPACE);
		if (snapshot.isEmpty()) {
			return;
		}
		TeleportHandoff handoff = TeleportCodec.decode(snapshot.get().payload());
		if (handoff.status() == TeleportHandoffStatus.CONSUMED
				|| handoff.status() == TeleportHandoffStatus.FAILED
				|| handoff.status() == TeleportHandoffStatus.CANCELLED
				|| handoff.status() == TeleportHandoffStatus.EXPIRED) {
			return;
		}
		Instant now = Instant.now();
		if (handoff.consumedAt() != null && handoff.ackedAt() == null
				&& Duration.between(handoff.consumedAt(), now).compareTo(ACK_STALE_AFTER) > 0) {
			markFailed(playerId, playerName, handoff, "ack stale after target arrival", true, "ack_stale");
			return;
		}
		if (handoff.lastUpdatedAt() != null && handoff.consumedAt() == null
				&& Duration.between(handoff.lastUpdatedAt(), now).compareTo(PREPARING_STALE_AFTER) > 0) {
			markFailed(playerId, playerName, handoff, "pending stale after prepare", true, "pending_stale");
			return;
		}
		if (handoff.expiresAt().isBefore(now)) {
			markFailed(playerId, playerName, handoff, "handoff expired during reconcile", true, "expired");
		}
	}

	public void protectOnShutdown() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			try {
				Optional<PlayerSnapshot> snapshot = api.loadPlayerData(player.getUniqueId(), NAMESPACE);
				if (snapshot.isEmpty()) {
					continue;
				}
				TeleportHandoff handoff = TeleportCodec.decode(snapshot.get().payload());
				if (handoff.status() == TeleportHandoffStatus.PENDING) {
					markFailed(player.getUniqueId(), player.getName(), handoff, "source server shutting down during pending handoff", true, "shutdown_guard");
				}
			} catch (Exception exception) {
				logger.warning("插件关闭时保护跨服 handoff 失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
			}
		}
	}

	public void recoverRollbackOnJoin(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				if (!hasRollbackSnapshot(player.getUniqueId())) {
					return;
				}
				restoreRollbackSnapshots(player.getUniqueId(), player);
				clearRollbackSnapshots(player.getUniqueId());
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					player.sendTitle("§e已恢复玩家状态", "§7检测到未完成跨服传送回滚", 5, 40, 10);
					player.sendActionBar(Component.text("已自动恢复上次跨服前的状态"));
					player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7F, 1.0F);
				});
			} catch (Exception exception) {
				logger.warning("玩家上线时恢复 rollback 快照失败: player=" + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public void flushPlayerData(Player player) {
		inventorySyncService.savePlayerData(player);
		playerStateSyncService.savePlayerState(player);
		if (playerPermissionSyncService != null) {
			playerPermissionSyncService.savePermissions(player);
		}
		if (homesSyncService != null) {
			homesSyncService.savePlayerHomes(player);
		}
	}

	private void saveRollbackSnapshot(Player player) throws Exception {
		api.savePlayerData(player.getUniqueId(), ROLLBACK_INVENTORY_NAMESPACE, inventorySyncService.captureInventoryPayload(player));
		api.savePlayerData(player.getUniqueId(), ROLLBACK_ENDER_CHEST_NAMESPACE, inventorySyncService.captureEnderChestPayload(player));
		api.savePlayerData(player.getUniqueId(), ROLLBACK_PLAYER_STATE_NAMESPACE, playerStateSyncService.captureStatePayload(player));
	}

	private boolean hasRollbackSnapshot(UUID playerId) throws Exception {
		return api.loadPlayerData(playerId, ROLLBACK_INVENTORY_NAMESPACE).isPresent()
				|| api.loadPlayerData(playerId, ROLLBACK_ENDER_CHEST_NAMESPACE).isPresent()
				|| api.loadPlayerData(playerId, ROLLBACK_PLAYER_STATE_NAMESPACE).isPresent();
	}

	private void restoreRollbackSnapshots(UUID playerId, Player player) throws Exception {
		Optional<PlayerSnapshot> inventorySnapshot = api.loadPlayerData(playerId, ROLLBACK_INVENTORY_NAMESPACE);
		Optional<PlayerSnapshot> enderChestSnapshot = api.loadPlayerData(playerId, ROLLBACK_ENDER_CHEST_NAMESPACE);
		Optional<PlayerSnapshot> stateSnapshot = api.loadPlayerData(playerId, ROLLBACK_PLAYER_STATE_NAMESPACE);
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			inventorySyncService.applyPayloads(player,
					inventorySnapshot.map(PlayerSnapshot::payload).orElse(null),
					enderChestSnapshot.map(PlayerSnapshot::payload).orElse(null));
			playerStateSyncService.applyPayload(player, stateSnapshot.map(PlayerSnapshot::payload).orElse(null));
		});
	}

	private void clearRollbackSnapshots(UUID playerId) throws Exception {
		storageProvider.deletePlayerData(playerId, ROLLBACK_INVENTORY_NAMESPACE);
		storageProvider.deletePlayerData(playerId, ROLLBACK_ENDER_CHEST_NAMESPACE);
		storageProvider.deletePlayerData(playerId, ROLLBACK_PLAYER_STATE_NAMESPACE);
		lastTeleportTime.remove(playerId);
	}

	private void saveHandoffSync(UUID playerId, String playerName, TeleportHandoff handoff, String eventType, String detail) throws Exception {
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
	}

	private void markFailed(UUID playerId, String playerName, TeleportHandoff handoff, String reason, boolean clearPreparedTransfer, String historyStatus) throws Exception {
		Instant failedAt = Instant.now();
		TeleportHandoff failed = new TeleportHandoff(
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
				TeleportHandoffStatus.FAILED,
				handoff.gatewaySentAt(),
				handoff.consumedAt(),
				handoff.consumedServerId(),
				handoff.ackedAt(),
				handoff.ackedByServerId(),
				handoff.rollbackRequestId(),
				"PENDING",
				null,
				handoff.recoveryState(),
				clearPreparedTransfer ? "CLEARED" : handoff.preparedTransferState(),
				clearPreparedTransfer ? failedAt : handoff.preparedTransferClearedAt(),
				failedAt,
				reason
		);
		saveHandoffSync(playerId, playerName, failed, historyStatus, reason);
		if (clearPreparedTransfer) {
			api.sessionService().clearPreparedTransfer(playerId);
		}
		markRollbackPending(playerId, playerName, failed, historyStatus, reason);
	}

	private void markRollbackPending(UUID playerId, String playerName, TeleportHandoff handoff, String historyStatus, String detail) throws Exception {
		storageProvider.appendTransferHistory(
				handoff.requestId(),
				playerId,
				playerName,
				handoff.sourceServerId(),
				handoff.targetServerId(),
				historyStatus + ".rollback",
				handoff.status(),
				detail
		);
	}
}
