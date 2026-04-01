package org.xiaoziyi.crossserver.ui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.bootstrap.CrossServerPlugin;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.config.RouteTableService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RouteEditSessionService {
	private final JavaPlugin plugin;
	private final RouteTableService routeTableService;
	private final PluginConfiguration configuration;
	private final Map<UUID, RouteEditSession> sessions = new ConcurrentHashMap<>();

	public RouteEditSessionService(JavaPlugin plugin, RouteTableService routeTableService, PluginConfiguration configuration) {
		this.plugin = plugin;
		this.routeTableService = routeTableService;
		this.configuration = configuration;
	}

	public void beginEdit(Player player, String serverId, int returnPage) {
		sessions.put(player.getUniqueId(), RouteEditSession.edit(serverId, returnPage));
		player.closeInventory();
		String current = routeTableService.mergedRoutes(configuration).get(serverId);
		player.sendMessage("§a正在编辑路由: §f" + serverId);
		player.sendMessage("§7当前目标: §f" + valueOrDash(current));
		player.sendMessage("§7在聊天栏输入新的代理服名。输入 §fcancel §7取消，输入 §fremove §7删除共享覆盖。");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7F, 1.1F);
	}

	public void beginCreate(Player player, int returnPage) {
		sessions.put(player.getUniqueId(), RouteEditSession.create(returnPage));
		player.closeInventory();
		player.sendMessage("§a正在新增共享路由");
		player.sendMessage("§7请在聊天栏输入: §f<serverId> <proxyTarget>");
		player.sendMessage("§7例如: §fserver-3 lobby-03");
		player.sendMessage("§7输入 §fcancel §7取消。");
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7F, 1.1F);
	}

	public boolean hasSession(UUID playerId) {
		return sessions.containsKey(playerId);
	}

	public void handleChat(Player player, String message) {
		RouteEditSession session = sessions.remove(player.getUniqueId());
		if (session == null) {
			return;
		}
		String input = message.trim();
		if (input.equalsIgnoreCase("cancel")) {
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				player.sendMessage("§e已取消路由编辑。");
				player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 0.9F);
				openMenu(player, session.returnPage());
			});
			return;
		}
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				if (session.createMode()) {
					handleCreate(player, session, input);
					return;
				}
				if (input.equalsIgnoreCase("remove")) {
					boolean removed = routeTableService.removeSharedRoute(session.serverId(), player.getName());
					plugin.getServer().getScheduler().runTask(plugin, () -> {
						player.sendMessage(removed
								? "§a已移除共享路由覆盖: §f" + session.serverId()
								: "§e共享路由中没有该覆盖项: §f" + session.serverId());
						player.sendMessage("§e如需本节点立即生效，请执行 /crossserver reload。");
						player.playSound(player.getLocation(), removed ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.UI_BUTTON_CLICK, 0.7F, 1.1F);
						openMenu(player, session.returnPage());
					});
					return;
				}
				routeTableService.setSharedRoute(session.serverId(), input, player.getName());
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					player.sendMessage("§a已保存共享路由: §f" + session.serverId() + " §8-> §f" + input);
					player.sendMessage("§e如需本节点立即生效，请执行 /crossserver reload。");
					player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.15F);
					openMenu(player, session.returnPage());
				});
			} catch (Exception exception) {
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					player.sendMessage("§c保存共享路由失败: " + exception.getMessage());
					player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
					openMenu(player, session.returnPage());
				});
			}
		});
	}

	private void handleCreate(Player player, RouteEditSession session, String input) throws Exception {
		String[] parts = input.split("\\s+", 2);
		if (parts.length < 2) {
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				player.sendMessage("§c新增格式不正确，请输入: <serverId> <proxyTarget>");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
				beginCreate(player, session.returnPage());
			});
			return;
		}
		routeTableService.setSharedRoute(parts[0], parts[1], player.getName());
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			player.sendMessage("§a已新增共享路由: §f" + parts[0] + " §8-> §f" + parts[1]);
			player.sendMessage("§e如需本节点立即生效，请执行 /crossserver reload。");
			player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.15F);
			openMenu(player, session.returnPage());
		});
	}

	private void openMenu(Player player, int returnPage) {
		RouteConfigMenuService menuService = ((CrossServerPlugin) plugin).getRouteConfigMenuService();
		if (menuService != null && player.isOnline()) {
			menuService.openMenu(player, returnPage);
		}
	}

	private String valueOrDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}

	private record RouteEditSession(String serverId, int returnPage, boolean createMode) {
		private static RouteEditSession edit(String serverId, int returnPage) {
			return new RouteEditSession(serverId, returnPage, false);
		}

		private static RouteEditSession create(int returnPage) {
			return new RouteEditSession(null, returnPage, true);
		}
	}
}
