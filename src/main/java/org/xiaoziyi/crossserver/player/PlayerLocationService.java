package org.xiaoziyi.crossserver.player;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.teleport.TeleportTarget;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class PlayerLocationService {
	public static final String NAMESPACE = "player-location";
	private static final long FRESH_SECONDS = 15L;

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;
	private final String serverId;

	public PlayerLocationService(JavaPlugin plugin, Logger logger, CrossServerApi api, String serverId) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
		this.serverId = serverId;
	}

	public void savePlayerLocation(Player player, boolean online) {
		Location location = player.getLocation();
		World world = location.getWorld();
		if (world == null) {
			return;
		}
		PlayerLocationSnapshot snapshot = new PlayerLocationSnapshot(
				serverId,
				world.getName(),
				location.getX(),
				location.getY(),
				location.getZ(),
				location.getYaw(),
				location.getPitch(),
				online,
				Instant.now()
		);
		String payload = PlayerLocationCodec.encode(snapshot);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				api.savePlayerData(player.getUniqueId(), NAMESPACE, payload);
			} catch (Exception exception) {
				logger.warning("保存玩家位置失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public Optional<PlayerLocationSnapshot> getPlayerLocation(UUID playerId) {
		try {
			Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, NAMESPACE);
			if (snapshot.isEmpty() || snapshot.get().payload() == null || snapshot.get().payload().isBlank()) {
				return Optional.empty();
			}
			return Optional.of(PlayerLocationCodec.decode(snapshot.get().payload()));
		} catch (Exception exception) {
			logger.warning("读取玩家位置失败: " + playerId + " -> " + exception.getMessage());
			return Optional.empty();
		}
	}

	public TeleportTarget toTeleportTarget(PlayerLocationSnapshot snapshot) {
		return new TeleportTarget(snapshot.serverId(), snapshot.world(), snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
	}

	public boolean isFresh(PlayerLocationSnapshot snapshot) {
		return snapshot != null
				&& snapshot.updatedAt() != null
				&& snapshot.updatedAt().isAfter(Instant.now().minusSeconds(FRESH_SECONDS));
	}
}
