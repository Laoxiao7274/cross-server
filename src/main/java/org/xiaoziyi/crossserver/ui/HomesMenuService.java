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
import org.xiaoziyi.crossserver.homes.HomeEntry;
import org.xiaoziyi.crossserver.homes.HomesSyncService;

import java.util.Comparator;
import java.util.List;

public final class HomesMenuService {
	public static final String MENU_PERMISSION = "crossserver.homes.menu";
	private static final int PAGE_SIZE = 45;
	private final JavaPlugin plugin;
	private final HomesSyncService homesSyncService;

	public HomesMenuService(JavaPlugin plugin, HomesSyncService homesSyncService) {
		this.plugin = plugin;
		this.homesSyncService = homesSyncService;
	}

	public void openMenu(Player player, int page) {
		List<HomeEntry> homes = homesSyncService.listHomes(player.getUniqueId()).stream()
				.sorted(Comparator.comparing(HomeEntry::name))
				.toList();
		int totalPages = Math.max(1, (homes.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		int currentPage = Math.max(1, Math.min(page, totalPages));
		HomesMenuHolder holder = new HomesMenuHolder(player.getUniqueId(), currentPage);
		Inventory inventory = Bukkit.createInventory(holder, 54, "✦ 家园菜单 ✦ " + currentPage + "/" + totalPages);
		holder.bind(inventory);
		decorateMainArea(inventory);
		String defaultHome = homesSyncService.getDefaultHome(player.getUniqueId());
		int fromIndex = (currentPage - 1) * PAGE_SIZE;
		int toIndex = Math.min(homes.size(), fromIndex + PAGE_SIZE);
		for (int index = fromIndex; index < toIndex; index++) {
			HomeEntry home = homes.get(index);
			inventory.setItem(index - fromIndex, createHomeItem(home, defaultHome, player));
		}
		if (homes.isEmpty()) {
			inventory.setItem(22, createControl(Material.BOOK, "§e还没有家园", List.of(
					"§7你还没有设置任何家园。",
					"§a使用 /sethome <name> 创建第一个家园"
			)));
		}
		decorateBottomBar(inventory);
		inventory.setItem(45, createControl(currentPage > 1 ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e上一页", currentPage > 1 ? List.of("§7点击查看上一页") : List.of("§8已经是第一页")));
		inventory.setItem(47, createControl(Material.BOOK, "§b统计", List.of(
				"§7家园数量: §f" + homes.size(),
				"§7默认家园: §f" + (defaultHome == null ? "-" : defaultHome),
				"§7页码: §f" + currentPage + "/" + totalPages
		)));
		inventory.setItem(49, createControl(Material.COMPASS, "§bHomes 菜单", List.of(
				"§7左键: 传送",
				"§7右键: 设为默认",
				"§7Shift+右键: 删除",
				"§8跨服家园会自动触发 handoff"
		)));
		inventory.setItem(51, createControl(Material.ENDER_CHEST, "§d当前位置", List.of(
				"§7服务器: §f" + player.getServer().getName(),
				"§7世界: §f" + (player.getWorld() == null ? "-" : player.getWorld().getName())
		)));
		inventory.setItem(53, createControl(currentPage < totalPages ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e下一页", currentPage < totalPages ? List.of("§7点击查看下一页") : List.of("§8已经是最后一页")));
		player.openInventory(inventory);
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.1F);
	}

	public void handleClick(Player player, HomesMenuHolder holder, int slot, boolean leftClick, boolean rightClick, boolean shiftClick, boolean middleClick) {
		List<HomeEntry> homes = homesSyncService.listHomes(player.getUniqueId()).stream()
				.sorted(Comparator.comparing(HomeEntry::name))
				.toList();
		int totalPages = Math.max(1, (homes.size() + PAGE_SIZE - 1) / PAGE_SIZE);
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
		int homeIndex = (holder.page() - 1) * PAGE_SIZE + slot;
		if (homeIndex >= homes.size()) {
			return;
		}
		HomeEntry home = homes.get(homeIndex);
		if (shiftClick && rightClick) {
			if (!player.hasPermission("crossserver.homes.delete")) {
				player.sendMessage("§c你没有权限删除家园。");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
				return;
			}
			player.closeInventory();
			player.sendMessage(homesSyncService.deleteHome(player.getUniqueId(), home.name()));
			player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7F, 1.0F);
			plugin.getServer().getScheduler().runTaskLater(plugin, () -> openMenu(player, holder.page()), 1L);
			return;
		}
		if (rightClick || middleClick) {
			if (!player.hasPermission("crossserver.homes.default")) {
				player.sendMessage("§c你没有权限设置默认家园。");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
				return;
			}
			player.sendMessage(homesSyncService.setDefaultHome(player.getUniqueId(), home.name()));
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7F, 1.3F);
			openMenu(player, holder.page());
			return;
		}
		if (!leftClick) {
			return;
		}
		if (!player.hasPermission("crossserver.homes.teleport")) {
			player.sendMessage("§c你没有权限传送到家园。");
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
			return;
		}
		player.closeInventory();
		player.sendMessage(homesSyncService.teleportHome(player, home.name()));
	}

	private ItemStack createHomeItem(HomeEntry home, String defaultHome, Player player) {
		Material material;
		if (home.name().equals(defaultHome)) {
			material = Material.NETHER_STAR;
		} else if (home.serverId().equalsIgnoreCase(player.getServer().getName())) {
			material = Material.LODESTONE;
		} else {
			material = Material.ENDER_PEARL;
		}
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName((home.name().equals(defaultHome) ? "§e✦ " : "§a» ") + home.name() + (home.name().equals(defaultHome) ? " §e(默认)" : ""));
		meta.setLore(List.of(
				"§7服务器: §f" + home.serverId(),
				"§7世界: §f" + home.world(),
				"§7坐标: §f" + (int) home.x() + ", " + (int) home.y() + ", " + (int) home.z(),
				home.serverId().equalsIgnoreCase(player.getServer().getName()) ? "§a当前服内家园" : "§d跨服家园",
				"",
				player.hasPermission("crossserver.homes.teleport") ? "§a左键传送" : "§8无传送权限",
				player.hasPermission("crossserver.homes.default") ? "§e右键设为默认" : "§8无默认家园权限",
				player.hasPermission("crossserver.homes.delete") ? "§cShift+右键删除" : "§8无删除权限"
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
