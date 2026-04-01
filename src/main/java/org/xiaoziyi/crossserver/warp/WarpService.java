package org.xiaoziyi.crossserver.warp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;
import org.xiaoziyi.crossserver.teleport.CrossServerTeleportService;
import org.xiaoziyi.crossserver.teleport.TeleportCause;
import org.xiaoziyi.crossserver.teleport.TeleportInitiationResult;
import org.xiaoziyi.crossserver.teleport.TeleportTarget;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class WarpService {
	public static final String NAMESPACE = "warps";
	public static final String DATA_KEY = "teleport.warps";
	private static final int MAX_WARP_NAME_LENGTH = 24;

	private final JavaPlugin plugin;
	private final Logger logger;
	private final CrossServerApi api;
	private final String serverId;
	private final CrossServerTeleportService teleportService;
	private final AtomicReference<WarpsSnapshot> cache = new AtomicReference<>(new WarpsSnapshot(Map.of()));

	public WarpService(JavaPlugin plugin, Logger logger, CrossServerApi api, String serverId, CrossServerTeleportService teleportService) {
		this.plugin = plugin;
		this.logger = logger;
		this.api = api;
		this.serverId = serverId;
		this.teleportService = teleportService;
		api.registerSyncListener(NAMESPACE, message -> refreshAsync());
		refreshAsync();
	}

	public void refreshAsync() {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				cache.set(loadSnapshot());
			} catch (Exception exception) {
				logger.warning("刷新 Warp 数据失败: " + exception.getMessage());
			}
		});
	}

	public List<WarpEntry> listWarps() {
		return cache.get().warps().values().stream()
				.sorted(Comparator.comparing(WarpEntry::name))
				.toList();
	}

	public String setWarp(Player player, String inputName) {
		String name = normalizeWarpName(inputName);
		if (name == null) {
			return "§cWarp 名称只能包含字母、数字、下划线和短横线，且长度不能超过 24。";
		}
		Location location = player.getLocation();
		World world = location.getWorld();
		if (world == null) {
			return "§c无法读取当前位置世界。";
		}
		try {
			WarpsSnapshot snapshot = loadSnapshot();
			Map<String, WarpEntry> warps = new LinkedHashMap<>(snapshot.warps());
			Instant now = Instant.now();
			WarpEntry previous = warps.get(name);
			warps.put(name, new WarpEntry(
					name,
					serverId,
					world.getName(),
					location.getX(),
					location.getY(),
					location.getZ(),
					location.getYaw(),
					location.getPitch(),
					player.getName(),
					previous != null ? previous.createdAt() : now,
					now
			));
				WarpsSnapshot updated = new WarpsSnapshot(Map.copyOf(warps));
				saveSnapshot(updated);
				cache.set(updated);
				return "§a已设置 Warp: §f" + name;
			} catch (Exception exception) {
				logger.warning("设置 Warp 失败: " + name + " -> " + exception.getMessage());
				return "§c设置 Warp 失败，请稍后重试。";
			}
	}

	public String deleteWarp(String inputName, String actorName) {
		String name = normalizeWarpName(inputName);
		if (name == null) {
			return "§c无效的 Warp 名称。";
		}
		try {
			WarpsSnapshot snapshot = loadSnapshot();
			Map<String, WarpEntry> warps = new LinkedHashMap<>(snapshot.warps());
			if (warps.remove(name) == null) {
				return "§c未找到 Warp: " + inputName;
			}
			WarpsSnapshot updated = new WarpsSnapshot(Map.copyOf(warps));
			saveSnapshot(updated);
			cache.set(updated);
			return "§a已删除 Warp: §f" + name;
		} catch (Exception exception) {
			logger.warning("删除 Warp 失败: " + name + " by " + actorName + " -> " + exception.getMessage());
			return "§c删除 Warp 失败，请稍后重试。";
		}
	}

	public String teleportWarp(Player player, String inputName) {
		if (inputName == null || inputName.isBlank()) {
			List<WarpEntry> warps = listWarps();
			if (warps.isEmpty()) {
				return "§e当前还没有可用的 Warp。";
			}
			return "§e可用 Warp: §f" + String.join("§7, §f", warps.stream().map(WarpEntry::name).toList());
		}
		String name = normalizeWarpName(inputName);
		if (name == null) {
			return "§c无效的 Warp 名称。";
		}
		WarpEntry warp = cache.get().warps().get(name);
		if (warp == null) {
			return "§c未找到 Warp: " + inputName;
		}
		if (!serverId.equalsIgnoreCase(warp.serverId())) {
			TeleportInitiationResult result = teleportService.requestTeleport(player, toTeleportTarget(warp), TeleportCause.WARP, warp.name());
			return result.message();
		}
		World world = plugin.getServer().getWorld(warp.world());
		if (world == null) {
			return "§c目标世界不存在: " + warp.world();
		}
		player.teleport(new Location(world, warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch()));
		return "§a已传送到 Warp: §f" + warp.name();
	}

	private TeleportTarget toTeleportTarget(WarpEntry warp) {
		return new TeleportTarget(warp.serverId(), warp.world(), warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());
	}

	private WarpsSnapshot loadSnapshot() throws Exception {
		Optional<GlobalSnapshot> snapshot = api.loadGlobalData(NAMESPACE, DATA_KEY);
		if (snapshot.isEmpty() || snapshot.get().payload() == null || snapshot.get().payload().isBlank()) {
			return new WarpsSnapshot(Map.of());
		}
		WarpsSnapshot decoded = WarpsCodec.decode(snapshot.get().payload());
		return new WarpsSnapshot(decoded.warps() == null ? Map.of() : Map.copyOf(decoded.warps()));
	}

	private void saveSnapshot(WarpsSnapshot snapshot) throws Exception {
		api.saveGlobalData(NAMESPACE, DATA_KEY, WarpsCodec.encode(snapshot));
	}

	private String normalizeWarpName(String name) {
		if (name == null) {
			return null;
		}
		String normalized = name.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank() || normalized.length() > MAX_WARP_NAME_LENGTH) {
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
