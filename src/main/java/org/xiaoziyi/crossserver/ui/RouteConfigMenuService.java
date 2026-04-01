package org.xiaoziyi.crossserver.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.bootstrap.CrossServerPlugin;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.config.RouteTableService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RouteConfigMenuService {
	private static final int PAGE_SIZE = 45;
	private final JavaPlugin plugin;
	private final RouteTableService routeTableService;
	private final PluginConfiguration configuration;
	private final RouteEditSessionService routeEditSessionService;

	public RouteConfigMenuService(JavaPlugin plugin, RouteTableService routeTableService, PluginConfiguration configuration, RouteEditSessionService routeEditSessionService) {
		this.plugin = plugin;
		this.routeTableService = routeTableService;
		this.configuration = configuration;
		this.routeEditSessionService = routeEditSessionService;
	}

	public void openMenu(Player viewer) {
		openMenu(viewer, 1);
	}

	public void openMenu(Player viewer, int page) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Map<String, String> sharedRoutes = routeTableService.loadSharedRoutes();
				Map<String, String> mergedRoutes = routeTableService.mergedRoutes(configuration);
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					viewer.openInventory(createInventory(page, sharedRoutes, mergedRoutes));
					viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.1F);
				});
			} catch (Exception exception) {
				plugin.getServer().getScheduler().runTask(plugin, () -> viewer.sendMessage("§c打开路由菜单失败: " + exception.getMessage()));
			}
		});
	}

	public void handleClick(Player player, RouteConfigMenuHolder holder, int slot, boolean leftClick, boolean rightClick) {
		if (slot == 45 && holder.page() > 1) {
			openMenu(player, holder.page() - 1);
			return;
		}
		if (slot == 49) {
			openMenu(player, holder.page());
			return;
		}
		if (slot == 51) {
			handleReload(player, holder.page());
			return;
		}
		if (slot == 53) {
			openMenu(player, holder.page() + 1);
			return;
		}
		if (slot == 22) {
			player.closeInventory();
			return;
		}
		if (slot == 50) {
			routeEditSessionService.beginCreate(player, holder.page());
			return;
		}
		String serverId = holder.targetAt(slot);
		if (serverId == null) {
			return;
		}
		if (rightClick) {
			routeEditSessionService.beginEdit(player, serverId, holder.page());
			player.sendMessage("§7提示: 输入 §fremove §7可删除共享覆盖。若本地 YAML 有基线，会回退到本地值。");
			return;
		}
		if (leftClick) {
			routeEditSessionService.beginEdit(player, serverId, holder.page());
		}
	}

	private Inventory createInventory(int page, Map<String, String> sharedRoutes, Map<String, String> mergedRoutes) {
		RouteConfigMenuHolder holder = new RouteConfigMenuHolder(page);
		Inventory inventory = Bukkit.createInventory(holder, 54, "✦ 路由管理 ✦ 第 " + page + " 页");
		holder.bind(inventory);
		fillBackground(inventory);
		List<String> serverIds = collectServerIds(sharedRoutes, mergedRoutes);
		int totalPages = Math.max(1, (serverIds.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		int currentPage = Math.min(Math.max(1, page), totalPages);
		int fromIndex = (currentPage - 1) * PAGE_SIZE;
		int toIndex = Math.min(serverIds.size(), fromIndex + PAGE_SIZE);
		List<String> pageEntries = serverIds.subList(fromIndex, toIndex);
		for (int i = 0; i < pageEntries.size(); i++) {
			String serverId = pageEntries.get(i);
			String proxyTarget = mergedRoutes.get(serverId);
			boolean shared = sharedRoutes.containsKey(serverId);
			holder.bindTarget(i, serverId);
			inventory.setItem(i, item(shared ? Material.ENDER_EYE : Material.COMPASS,
					(shared ? "§b" : "§a") + serverId,
					List.of(
							"§7代理目标: §f" + valueOrDash(proxyTarget),
							"§7来源: §f" + routeSourceLabel(serverId, sharedRoutes),
							"§7左键编辑代理目标",
							"§7右键进入删除覆盖输入",
							"§8保存后需 /crossserver reload 生效"
					)));
		}
		inventory.setItem(45, item(Material.ARROW, "§e上一页", List.of("§7当前页: §f" + currentPage + "§7/§f" + totalPages)));
		inventory.setItem(49, item(Material.CLOCK, "§b刷新", List.of("§7重新加载共享路由与本地合并结果")));
		inventory.setItem(50, item(Material.ANVIL, "§a新增路由", List.of("§7通过聊天输入新增 serverId 与代理目标", "§8格式: serverId proxyTarget")));
		inventory.setItem(51, item(Material.REDSTONE_TORCH, "§e重载本节点", List.of("§7立即执行 /crossserver reload", "§8使共享路由在本节点生效")));
		inventory.setItem(53, item(Material.ARROW, "§e下一页", List.of("§7当前页: §f" + currentPage + "§7/§f" + totalPages)));
		inventory.setItem(22, item(Material.OAK_DOOR, "§7关闭", List.of("§7关闭菜单")));
		return inventory;
	}

	private List<String> collectServerIds(Map<String, String> sharedRoutes, Map<String, String> mergedRoutes) {
		Set<String> ids = new LinkedHashSet<>();
		ids.addAll(mergedRoutes.keySet());
		ids.addAll(sharedRoutes.keySet());
		return new ArrayList<>(ids);
	}

	private void fillBackground(Inventory inventory) {
		ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
		for (int slot = 45; slot <= 53; slot++) {
			inventory.setItem(slot, filler);
		}
	}

	private ItemStack item(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private String routeSourceLabel(String serverId, Map<String, String> sharedRoutes) {
		boolean local = configuration.teleport().gateway().serverMap().containsKey(serverId);
		boolean shared = sharedRoutes.containsKey(serverId);
		if (local && shared) {
			return "本地 + 共享覆盖";
		}
		if (shared) {
			return "仅共享";
		}
		return "仅本地";
	}

	private void handleReload(Player player, int returnPage) {
		if (!player.hasPermission("crossserver.reload")) {
			player.sendMessage("§c你没有权限重载本节点。");
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
			return;
		}
		player.closeInventory();
		player.sendMessage("§e正在重载 CrossServer...");
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				((CrossServerPlugin) plugin).reloadPlugin();
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					player.sendMessage("§aCrossServer 重载成功。");
					player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.15F);
					if (player.isOnline()) {
						openMenu(player, returnPage);
					}
				});
			} catch (Exception exception) {
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					player.sendMessage("§cCrossServer 重载失败: " + exception.getMessage());
					player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
					if (player.isOnline()) {
						openMenu(player, returnPage);
					}
				});
			}
		});
	}

	private String valueOrDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
