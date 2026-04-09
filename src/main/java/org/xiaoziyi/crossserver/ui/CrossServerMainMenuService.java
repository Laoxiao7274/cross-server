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

import java.util.List;

public final class CrossServerMainMenuService {
	private final JavaPlugin plugin;
	private final HomesMenuService homesMenuService;
	private final WarpMenuService warpMenuService;
	private final RouteConfigMenuService routeConfigMenuService;
	private final TransferAdminMenuService transferAdminMenuService;

	public CrossServerMainMenuService(JavaPlugin plugin, HomesMenuService homesMenuService, WarpMenuService warpMenuService, RouteConfigMenuService routeConfigMenuService, TransferAdminMenuService transferAdminMenuService) {
		this.plugin = plugin;
		this.homesMenuService = homesMenuService;
		this.warpMenuService = warpMenuService;
		this.routeConfigMenuService = routeConfigMenuService;
		this.transferAdminMenuService = transferAdminMenuService;
	}

	public void openMenu(Player player) {
		CrossServerMainMenuHolder holder = new CrossServerMainMenuHolder();
		Inventory inventory = Bukkit.createInventory(holder, 27, "✦ CrossServer 总菜单 ✦");
		holder.bind(inventory);
		fill(inventory);
		inventory.setItem(10, createEntry(player, homesMenuService != null && player.hasPermission("crossserver.homes.list"), Material.OAK_DOOR, "§a家园菜单", List.of("§7打开 /homes 菜单", "§8适合玩家快速回家")));
		inventory.setItem(12, createEntry(player, warpMenuService != null && player.hasPermission("crossserver.warps.list"), Material.COMPASS, "§bWarp 菜单", List.of("§7打开 /warp 菜单", "§8查看并传送到全局地标")));
		inventory.setItem(14, createEntry(player, routeConfigMenuService != null && player.hasPermission("crossserver.route.view"), Material.MAP, "§e路由管理", List.of("§7打开共享路由管理菜单", "§8适合管理员调整跨服路由")));
		inventory.setItem(16, createEntry(player, transferAdminMenuService != null && player.hasPermission("crossserver.transfer.menu"), Material.ENDER_EYE, "§dTransfer 管理", List.of("§7打开 recent transfer 菜单", "§8适合排查跨服问题")));
		inventory.setItem(22, createItem(Material.BOOK, "§f帮助分页", List.of("§7使用 /cs help [page] 查看更多命令", "§7别名: /cs")));
		player.openInventory(inventory);
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.05F);
	}

	public void handleClick(Player player, int slot) {
		switch (slot) {
			case 10 -> {
				player.closeInventory();
				if (homesMenuService != null) {
					homesMenuService.openMenu(player, 1);
				}
			}
			case 12 -> {
				player.closeInventory();
				if (warpMenuService != null) {
					warpMenuService.openMenu(player, 1);
				}
			}
			case 14 -> {
				player.closeInventory();
				if (routeConfigMenuService != null) {
					routeConfigMenuService.openMenu(player);
				}
			}
			case 16 -> {
				player.closeInventory();
				if (transferAdminMenuService != null) {
					transferAdminMenuService.openRecentMenu(player, 1);
				}
			}
			default -> {
			}
		}
	}

	private void fill(Inventory inventory) {
		for (int slot = 0; slot < inventory.getSize(); slot++) {
			inventory.setItem(slot, createItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
		}
	}

	private ItemStack createItem(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack createEntry(Player player, boolean available, Material enabledMaterial, String name, List<String> lore) {
		if (available) {
			return createItem(enabledMaterial, name, lore);
		}
		java.util.List<String> disabledLore = new java.util.ArrayList<>(lore);
		disabledLore.add("");
		disabledLore.add("§8当前不可用（模块关闭或无权限）");
		return createItem(Material.GRAY_DYE, "§8" + name.replace("§a", "").replace("§b", "").replace("§d", "").replace("§e", ""), disabledLore);
	}
}
