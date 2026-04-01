package org.xiaoziyi.crossserver.inventory;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class PlayerInventorySyncService {
	public static final String INVENTORY_NAMESPACE = "inventory";
	public static final String ENDER_CHEST_NAMESPACE = "enderchest";

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;

	public PlayerInventorySyncService(JavaPlugin plugin, Logger logger, CrossServerApi api) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
	}

	public void loadPlayerData(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				UUID playerId = player.getUniqueId();
				Optional<PlayerSnapshot> inventorySnapshot = api.loadPlayerData(playerId, INVENTORY_NAMESPACE);
				Optional<PlayerSnapshot> enderChestSnapshot = api.loadPlayerData(playerId, ENDER_CHEST_NAMESPACE);
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					if (!player.isOnline()) {
						return;
					}
					inventorySnapshot.ifPresent(snapshot -> InventorySnapshotCodec.apply(player.getInventory(), snapshot.payload()));
					enderChestSnapshot.ifPresent(snapshot -> InventorySnapshotCodec.apply(player.getEnderChest(), snapshot.payload()));
					player.updateInventory();
				});
			} catch (Exception exception) {
				logger.warning("加载玩家背包数据失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public void savePlayerData(Player player) {
		String inventoryPayload = captureInventoryPayload(player);
		String enderChestPayload = captureEnderChestPayload(player);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				UUID playerId = player.getUniqueId();
				api.savePlayerData(playerId, INVENTORY_NAMESPACE, inventoryPayload);
				api.savePlayerData(playerId, ENDER_CHEST_NAMESPACE, enderChestPayload);
			} catch (Exception exception) {
				logger.warning("保存玩家背包数据失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public String captureInventoryPayload(Player player) {
		return InventorySnapshotCodec.encode(player.getInventory().getContents());
	}

	public String captureEnderChestPayload(Player player) {
		return InventorySnapshotCodec.encode(player.getEnderChest().getContents());
	}

	public void applyPayloads(Player player, String inventoryPayload, String enderChestPayload) {
		if (!player.isOnline()) {
			return;
		}
		if (inventoryPayload != null && !inventoryPayload.isBlank()) {
			InventorySnapshotCodec.apply(player.getInventory(), inventoryPayload);
		}
		if (enderChestPayload != null && !enderChestPayload.isBlank()) {
			InventorySnapshotCodec.apply(player.getEnderChest(), enderChestPayload);
		}
		player.updateInventory();
	}
}
