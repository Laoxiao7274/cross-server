package org.xiaoziyi.crossserver.permission;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public final class PlayerPermissionSyncService {
	public static final String NAMESPACE = "player-permissions";

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;
	private final PermissionSyncAdapter adapter;

	public PlayerPermissionSyncService(JavaPlugin plugin, Logger logger, CrossServerApi api, PermissionSyncAdapter adapter) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
		this.adapter = adapter;
	}

	public void loadPermissions(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<PlayerSnapshot> snapshot = api.loadPlayerData(player.getUniqueId(), NAMESPACE);
				if (snapshot.isEmpty()) {
					return;
				}
				PlayerPermissionSnapshot permissionSnapshot = PermissionSnapshotCodec.decode(snapshot.get().payload());
				plugin.getServer().getScheduler().runTask(plugin, () -> applyPayload(player, permissionSnapshot));
			} catch (Exception exception) {
				logger.warning("加载玩家权限失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public void savePermissions(Player player) {
		String payload = capturePayload(player);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				api.savePlayerData(player.getUniqueId(), NAMESPACE, payload);
			} catch (Exception exception) {
				logger.warning("保存玩家权限失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public void savePermissionsSync(Player player) {
		try {
			api.savePlayerData(player.getUniqueId(), NAMESPACE, capturePayload(player));
		} catch (Exception exception) {
			logger.warning("保存玩家权限失败: " + player.getUniqueId() + " -> " + exception.getMessage());
		}
	}

	public String capturePayload(Player player) {
		Collection<String> captured = adapter.capturePermissions(player);
		Set<String> permissions = new LinkedHashSet<>();
		for (String permission : captured) {
			if (permission != null && !permission.isBlank()) {
				permissions.add(permission);
			}
		}
		return PermissionSnapshotCodec.encode(new PlayerPermissionSnapshot(List.copyOf(permissions)));
	}

	public void applyPayload(Player player, String payload) {
		if (payload == null || payload.isBlank()) {
			return;
		}
		applyPayload(player, PermissionSnapshotCodec.decode(payload));
	}

	public void clearPermissions(Player player) {
		if (!player.isOnline()) {
			return;
		}
		adapter.clearPermissions(player);
	}

	public String adapterName() {
		return adapter.name();
	}

	private void applyPayload(Player player, PlayerPermissionSnapshot snapshot) {
		if (!player.isOnline()) {
			return;
		}
		adapter.applyPermissions(player, snapshot.permissions());
	}
}
