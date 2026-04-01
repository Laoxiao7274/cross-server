package org.xiaoziyi.crossserver.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.warp.WarpEntry;
import org.xiaoziyi.crossserver.warp.WarpService;

import java.util.List;

public final class WarpMenuService {
	public static final String MENU_PERMISSION = "crossserver.warps.list";
	private static final int PAGE_SIZE = 45;

	private final JavaPlugin plugin;
	private final WarpService warpService;

	public WarpMenuService(JavaPlugin plugin, WarpService warpService) {
		this.plugin = plugin;
		this.warpService = warpService;
	}

	public void openMenu(Player player, int page) {
		List<WarpEntry> warps = warpService.listWarps();
		int totalPages = Math.max(1, (warps.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		int currentPage = Math.max(1, Math.min(page, totalPages));
		WarpMenuHolder holder = new WarpMenuHolder(player.getUniqueId(), currentPage);
		Inventory inventory = Bukkit.createInventory(holder, 54, "✦ Warp 菜单 ✦ " + currentPage + "/" + totalPages);
		holder.bind(inventory);
		decorateMainArea(inventory);
		int fromIndex = (currentPage - 1) * PAGE_SIZE;
		int toIndex = Math.min(warps.size(), fromIndex + PAGE_SIZE);
		for (int index = fromIndex; index < toIndex; index++) {
			WarpEntry warp = warps.get(index);
			int slot = index - fromIndex;
			holder.bindWarp(slot, warp.name());
			inventory.setItem(slot, createWarpItem(warp, player));
		}
		if (warps.isEmpty()) {
			inventory.setItem(22, createControl(Material.BOOK, "§e还没有 Warp", List.of(
					"§7当前没有可用的全局 Warp。",
					player.hasPermission("crossserver.warps.set") ? "§a使用 /setwarp <name> 创建第一个 Warp" : "§8你没有创建 Warp 的权限"
			)));
		}
		decorateBottomBar(inventory);
		inventory.setItem(45, createControl(currentPage > 1 ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e上一页", currentPage > 1 ? List.of("§7点击查看上一页") : List.of("§8已经是第一页")));
		inventory.setItem(47, createControl(Material.BOOK, "§b统计", List.of(
				"§7Warp 数量: §f" + warps.size(),
				"§7页码: §f" + currentPage + "/" + totalPages,
				"§7当前服务器: §f" + player.getServer().getName()
		)));
		inventory.setItem(49, createControl(Material.COMPASS, "§bWarp 菜单", List.of(
				player.hasPermission("crossserver.warps.teleport") ? "§a左键传送" : "§8无传送权限",
				player.hasPermission("crossserver.warps.delete") ? "§cShift+右键删除" : "§8无删除权限",
				"§8跨服 Warp 会自动触发 handoff"
		)));
		inventory.setItem(51, createControl(Material.ENDER_CHEST, "§d当前位置", List.of(
				"§7服务器: §f" + player.getServer().getName(),
				"§7世界: §f" + player.getWorld().getName()
		)));
		inventory.setItem(53, createControl(currentPage < totalPages ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e下一页", currentPage < totalPages ? List.of("§7点击查看下一页") : List.of("§8已经是最后一页")));
		player.openInventory(inventory);
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.1F);
	}

	public void handleClick(Player player, WarpMenuHolder holder, int slot, boolean leftClick, boolean rightClick, boolean shiftClick) {
		List<WarpEntry> warps = warpService.listWarps();
		int totalPages = Math.max(1, (warps.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		if (slot == 45) {
			if (holder.page() > 1) {
				openMenu(player, holder.page() - 1);
			}
			return;
		}
		if (slot == 53) {
			if (holder.page() < totalPages) {
				openMenu(player, holder.page() + 1);
			}
			return;
		}
		if (slot < 0 || slot >= PAGE_SIZE) {
			return;
		}
		String warpName = holder.warpAt(slot);
		if (warpName == null) {
			return;
		}
		if (shiftClick && rightClick) {
			if (!player.hasPermission("crossserver.warps.delete")) {
				player.sendMessage("§c你没有权限删除 Warp。");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
				return;
			}
			player.closeInventory();
			player.sendMessage(warpService.deleteWarp(warpName, player.getName()));
			player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7F, 1.0F);
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMenu(player, holder.page()), 1L);
			return;
		}
		if (!leftClick) {
			return;
		}
		if (!player.hasPermission("crossserver.warps.teleport")) {
			player.sendMessage("§c你没有权限使用 Warp。");
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
			return;
		}
		player.closeInventory();
		player.sendMessage(warpService.teleportWarp(player, warpName));
	}

	private ItemStack createWarpItem(WarpEntry warp, Player player) {
		Material material = warp.serverId().equalsIgnoreCase(player.getServer().getName()) ? Material.LODESTONE : Material.ENDER_PEARL;
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("§a» " + warp.name());
		meta.setLore(List.of(
				"§7服务器: §f" + warp.serverId(),
				"§7世界: §f" + warp.world(),
				"§7坐标: §f" + (int) warp.x() + ", " + (int) warp.y() + ", " + (int) warp.z(),
				"§7创建者: §f" + warp.createdBy(),
				warp.serverId().equalsIgnoreCase(player.getServer().getName()) ? "§a当前服 Warp" : "§d跨服 Warp",
				"",
				player.hasPermission("crossserver.warps.teleport") ? "§a左键传送" : "§8无传送权限",
				player.hasPermission("crossserver.warps.delete") ? "§cShift+右键删除" : "§8无删除权限"
		));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		return item;
	}

	private void decorateMainArea(Inventory inventory) {
		for (int slot = 0; slot < 45; slot++) {
			inventory.setItem(slot, createControl(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
		}
	}

	private void decorateBottomBar(Inventory inventory) {
		for (int slot = 45; slot < 54; slot++) {
			inventory.setItem(slot, createControl(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
		}
	}

	private ItemStack createControl(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
}
