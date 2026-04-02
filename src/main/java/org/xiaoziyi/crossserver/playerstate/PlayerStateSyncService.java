package org.xiaoziyi.crossserver.playerstate;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class PlayerStateSyncService {
	public static final String NAMESPACE = "player-state";

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;

	public PlayerStateSyncService(JavaPlugin plugin, Logger logger, CrossServerApi api) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
	}

	public void loadPlayerState(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				UUID playerId = player.getUniqueId();
				Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, NAMESPACE);
				if (snapshot.isEmpty()) {
					return;
				}
				PlayerStateSnapshot state = PlayerStateCodec.decode(snapshot.get().payload());
				plugin.getServer().getScheduler().runTask(plugin, () -> apply(player, state));
			} catch (Exception exception) {
				logger.warning("加载玩家状态失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public void savePlayerState(Player player) {
		String payload = captureStatePayload(player);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				api.savePlayerData(player.getUniqueId(), NAMESPACE, payload);
			} catch (Exception exception) {
				logger.warning("保存玩家状态失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public String captureStatePayload(Player player) {
		AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
		double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0D;
		PlayerStateSnapshot snapshot = new PlayerStateSnapshot(
				player.getHealth(),
				maxHealth,
				player.getFoodLevel(),
				player.getSaturation(),
				player.getExhaustion(),
				player.getLevel(),
				player.getExp(),
				player.getTotalExperience(),
				player.getFireTicks(),
				player.getRemainingAir(),
				player.getGameMode().name()
		);
		return PlayerStateCodec.encode(snapshot);
	}

	public void applyPayload(Player player, String payload) {
		if (payload == null || payload.isBlank()) {
			return;
		}
		apply(player, PlayerStateCodec.decode(payload));
	}

	private void apply(Player player, PlayerStateSnapshot state) {
		if (!player.isOnline()) {
			return;
		}
		AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
		double actualMaxHealth = state.maxHealth();
		if (maxHealthAttribute != null) {
			actualMaxHealth = Math.max(1.0D, state.maxHealth());
			maxHealthAttribute.setBaseValue(actualMaxHealth);
		}
		player.setHealth(Math.min(Math.max(0.0D, state.health()), actualMaxHealth));
		player.setFoodLevel(Math.max(0, Math.min(20, state.foodLevel())));
		player.setSaturation(Math.max(0.0F, state.saturation()));
		player.setExhaustion(Math.max(0.0F, state.exhaustion()));
		player.setLevel(Math.max(0, state.level()));
		player.setExp(Math.max(0.0F, Math.min(1.0F, state.exp())));
		player.setTotalExperience(Math.max(0, state.totalExperience()));
		player.setFireTicks(Math.max(0, state.fireTicks()));
		player.setRemainingAir(Math.max(0, state.remainingAir()));
		if (state.gameMode() != null && !state.gameMode().isBlank()) {
			try {
				player.setGameMode(GameMode.valueOf(state.gameMode()));
			} catch (IllegalArgumentException ignored) {
			}
		}
	}
}
