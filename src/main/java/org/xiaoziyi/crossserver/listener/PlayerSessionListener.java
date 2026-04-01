package org.xiaoziyi.crossserver.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.homes.HomesSyncService;
import org.xiaoziyi.crossserver.inventory.PlayerInventorySyncService;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.playerstate.PlayerStateSyncService;
import org.xiaoziyi.crossserver.session.SessionService;
import org.xiaoziyi.crossserver.storage.StorageProvider;
import org.xiaoziyi.crossserver.teleport.CrossServerTeleportService;
import org.xiaoziyi.crossserver.teleport.TeleportCodec;
import org.xiaoziyi.crossserver.teleport.TeleportHandoff;
import org.xiaoziyi.crossserver.teleport.TeleportHandoffStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSessionListener implements Listener {
	private final JavaPlugin plugin;
	private final SessionService sessionService;
	private final StorageProvider storageProvider;
	private final PlayerInventorySyncService inventorySyncService;
	private final PlayerStateSyncService playerStateSyncService;
	private final HomesSyncService homesSyncService;
	private final String kickMessage;
	private final Set<UUID> kickedPlayers;

	public PlayerSessionListener(
			JavaPlugin plugin,
			SessionService sessionService,
			StorageProvider storageProvider,
			PlayerInventorySyncService inventorySyncService,
			PlayerStateSyncService playerStateSyncService,
			HomesSyncService homesSyncService,
			String kickMessage
	) {
		this.plugin = plugin;
		this.sessionService = sessionService;
		this.storageProvider = storageProvider;
		this.inventorySyncService = inventorySyncService;
		this.playerStateSyncService = playerStateSyncService;
		this.homesSyncService = homesSyncService;
		this.kickMessage = kickMessage;
		this.kickedPlayers = ConcurrentHashMap.newKeySet();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
		try {
			storageProvider.savePlayerIdentity(event.getUniqueId(), event.getName());
			Optional<PlayerSnapshot> handoffSnapshot = storageProvider.loadPlayerData(event.getUniqueId(), CrossServerTeleportService.NAMESPACE);
			boolean success = false;
			if (handoffSnapshot.isPresent()) {
				TeleportHandoff handoff = TeleportCodec.decode(handoffSnapshot.get().payload());
				if (handoff.status() == TeleportHandoffStatus.PENDING
						&& handoff.targetServerId().equalsIgnoreCase(sessionService.getServerId())
						&& handoff.expiresAt().isAfter(Instant.now())
						&& handoff.sessionTransferToken() != null
						&& !handoff.sessionTransferToken().isBlank()) {
					success = sessionService.openTransferredSession(event.getUniqueId(), handoff.sessionTransferToken());
				}
			}
			if (!success) {
				success = sessionService.openPlayerSession(event.getUniqueId());
			}
			if (!success) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
			}
		} catch (Exception exception) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "跨服会话初始化失败，请稍后重试");
			plugin.getLogger().warning("初始化玩家会话失败: " + event.getUniqueId() + " -> " + exception.getMessage());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		inventorySyncService.loadPlayerData(event.getPlayer());
		playerStateSyncService.loadPlayerState(event.getPlayer());
		homesSyncService.loadPlayerHomes(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDeath(PlayerDeathEvent event) {
		inventorySyncService.savePlayerData(event.getPlayer());
		playerStateSyncService.savePlayerState(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onRespawn(PlayerRespawnEvent event) {
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			inventorySyncService.savePlayerData(event.getPlayer());
			playerStateSyncService.savePlayerState(event.getPlayer());
		}, 1L);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		if (!kickedPlayers.remove(event.getPlayer().getUniqueId())) {
			inventorySyncService.savePlayerData(event.getPlayer());
			playerStateSyncService.savePlayerState(event.getPlayer());
			homesSyncService.savePlayerHomes(event.getPlayer());
			homesSyncService.unloadPlayerHomes(event.getPlayer().getUniqueId());
			if (sessionService.isTransferDeparture(event.getPlayer().getUniqueId())) {
				sessionService.discardLocalSession(event.getPlayer().getUniqueId());
				return;
			}
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sessionService.closePlayerSession(event.getPlayer().getUniqueId()));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onKick(PlayerKickEvent event) {
		kickedPlayers.add(event.getPlayer().getUniqueId());
		inventorySyncService.savePlayerData(event.getPlayer());
		playerStateSyncService.savePlayerState(event.getPlayer());
		homesSyncService.savePlayerHomes(event.getPlayer());
		homesSyncService.unloadPlayerHomes(event.getPlayer().getUniqueId());
		if (sessionService.isTransferDeparture(event.getPlayer().getUniqueId())) {
			sessionService.discardLocalSession(event.getPlayer().getUniqueId());
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sessionService.closePlayerSession(event.getPlayer().getUniqueId()));
	}
}
