package org.xiaoziyi.crossserver.homes;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.i18n.Texts;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.teleport.CrossServerTeleportService;
import org.xiaoziyi.crossserver.teleport.TeleportCause;
import org.xiaoziyi.crossserver.teleport.TeleportInitiationResult;
import org.xiaoziyi.crossserver.teleport.TeleportTarget;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class HomesSyncService {
	public static final String NAMESPACE = "homes";
	private static final int MAX_HOME_NAME_LENGTH = 16;

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;
	private final String serverId;
	private final CrossServerTeleportService teleportService;
	private final Map<UUID, HomesSnapshot> cache;
	private final Texts texts;

	public HomesSyncService(JavaPlugin plugin, Logger logger, CrossServerApi api, String serverId, CrossServerTeleportService teleportService, Texts texts) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
		this.serverId = serverId;
		this.teleportService = teleportService;
		this.cache = new ConcurrentHashMap<>();
		this.texts = texts;
	}

	public void loadPlayerHomes(Player player) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<PlayerSnapshot> snapshot = api.loadPlayerData(player.getUniqueId(), NAMESPACE);
				HomesSnapshot homesSnapshot = snapshot.map(value -> HomesCodec.decode(value.payload())).orElseGet(this::emptySnapshot);
				cache.put(player.getUniqueId(), homesSnapshot);
			} catch (Exception exception) {
				logger.warning("加载家园数据失败: " + player.getUniqueId() + " -> " + exception.getMessage());
			}
		});
	}

	public void savePlayerHomes(Player player) {
		HomesSnapshot snapshot = cache.get(player.getUniqueId());
		if (snapshot == null) {
			return;
		}
		saveSnapshotAsync(player.getUniqueId(), snapshot);
	}

	public void savePlayerHomesSync(Player player) {
		HomesSnapshot snapshot = cache.get(player.getUniqueId());
		if (snapshot == null) {
			return;
		}
		try {
			api.savePlayerData(player.getUniqueId(), NAMESPACE, HomesCodec.encode(snapshot));
		} catch (Exception exception) {
			logger.warning("保存家园数据失败: " + player.getUniqueId() + " -> " + exception.getMessage());
		}
	}

	public void unloadPlayerHomes(UUID playerId) {
		cache.remove(playerId);
	}

	public boolean isLoaded(UUID playerId) {
		return cache.containsKey(playerId);
	}

	public List<HomeEntry> listHomes(UUID playerId) {
		HomesSnapshot snapshot = cache.get(playerId);
		if (snapshot == null) {
			return List.of();
		}
		return new ArrayList<>(snapshot.homes().values());
	}

	public String getDefaultHome(UUID playerId) {
		HomesSnapshot snapshot = cache.get(playerId);
		return snapshot == null ? null : snapshot.defaultHome();
	}

	public String setHome(Player player, String inputName) {
		HomesSnapshot snapshot = cache.get(player.getUniqueId());
		if (snapshot == null) {
			return texts.tr("homes.loading");
		}
		String name = normalizeHomeName(inputName);
		if (name == null) {
			return texts.tr("homes.name_invalid");
		}
		Location location = player.getLocation();
		World world = location.getWorld();
		if (world == null) {
			return texts.tr("homes.world_missing");
		}
		Map<String, HomeEntry> homes = new LinkedHashMap<>(snapshot.homes());
		Instant now = Instant.now();
		HomeEntry previous = homes.get(name);
		homes.put(name, new HomeEntry(
				name,
				serverId,
				world.getName(),
				location.getX(),
				location.getY(),
				location.getZ(),
				location.getYaw(),
				location.getPitch(),
				previous != null ? previous.createdAt() : now,
				now
		));
		String defaultHome = snapshot.defaultHome() != null ? snapshot.defaultHome() : name;
		HomesSnapshot updated = new HomesSnapshot(defaultHome, homes);
		cache.put(player.getUniqueId(), updated);
		saveSnapshotAsync(player.getUniqueId(), updated);
		return texts.tr("homes.set", name);
	}

	public String deleteHome(UUID playerId, String inputName) {
		HomesSnapshot snapshot = cache.get(playerId);
		if (snapshot == null) {
			return texts.tr("homes.loading");
		}
		String name = normalizeHomeName(inputName);
		if (name == null) {
			return texts.tr("homes.not_found", inputName);
		}
		Map<String, HomeEntry> homes = new LinkedHashMap<>(snapshot.homes());
		HomeEntry removed = homes.remove(name);
		if (removed == null) {
			return texts.tr("homes.not_found", inputName);
		}
		String defaultHome = snapshot.defaultHome();
		if (name.equals(defaultHome)) {
			defaultHome = homes.isEmpty() ? null : homes.keySet().iterator().next();
		}
		HomesSnapshot updated = new HomesSnapshot(defaultHome, homes);
		cache.put(playerId, updated);
		saveSnapshotAsync(playerId, updated);
		return texts.tr("homes.deleted", name);
	}

	public String setDefaultHome(UUID playerId, String inputName) {
		HomesSnapshot snapshot = cache.get(playerId);
		if (snapshot == null) {
			return texts.tr("homes.loading");
		}
		String name = normalizeHomeName(inputName);
		if (name == null || !snapshot.homes().containsKey(name)) {
			return texts.tr("homes.not_found", inputName);
		}
		HomesSnapshot updated = new HomesSnapshot(name, snapshot.homes());
		cache.put(playerId, updated);
		saveSnapshotAsync(playerId, updated);
		return texts.tr("homes.default_set", name);
	}

	public String teleportHome(Player player, String inputName) {
		HomesSnapshot snapshot = cache.get(player.getUniqueId());
		if (snapshot == null) {
			return texts.tr("homes.loading");
		}
		String name = inputName == null || inputName.isBlank() ? snapshot.defaultHome() : normalizeHomeName(inputName);
		if (name == null || name.isBlank()) {
			return texts.tr("homes.default_missing");
		}
		HomeEntry home = snapshot.homes().get(name);
		if (home == null) {
			return texts.tr("homes.not_found", name);
		}
		if (!serverId.equalsIgnoreCase(home.serverId())) {
			TeleportInitiationResult result = teleportService.requestTeleport(player, toTeleportTarget(home), TeleportCause.HOME, home.name());
			return result.message();
		}
		World world = plugin.getServer().getWorld(home.world());
		if (world == null) {
			return "§c" + home.world();
		}
		player.teleport(new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch()));
		return texts.tr("homes.teleport_local", home.name());
	}

	private TeleportTarget toTeleportTarget(HomeEntry home) {
		return new TeleportTarget(home.serverId(), home.world(), home.x(), home.y(), home.z(), home.yaw(), home.pitch());
	}

	private void saveSnapshotAsync(UUID playerId, HomesSnapshot snapshot) {
		String payload = HomesCodec.encode(snapshot);
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				api.savePlayerData(playerId, NAMESPACE, payload);
			} catch (Exception exception) {
				logger.warning("保存家园数据失败: " + playerId + " -> " + exception.getMessage());
			}
		});
	}

	private HomesSnapshot emptySnapshot() {
		return new HomesSnapshot(null, Map.of());
	}

	private String normalizeHomeName(String name) {
		if (name == null) {
			return null;
		}
		String normalized = name.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank() || normalized.length() > MAX_HOME_NAME_LENGTH) {
			return null;
		}
		for (int index = 0; index < normalized.length(); index++) {
			char current = normalized.charAt(index);
			if (!Character.isLetterOrDigit(current) && current != '_' && current != '-') {
				return null;
			}
		}
		return normalized;
	}
}
