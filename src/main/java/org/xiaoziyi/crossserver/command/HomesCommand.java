package org.xiaoziyi.crossserver.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xiaoziyi.crossserver.homes.HomeEntry;
import org.xiaoziyi.crossserver.homes.HomesSyncService;
import org.xiaoziyi.crossserver.i18n.Texts;
import org.xiaoziyi.crossserver.ui.HomesMenuService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HomesCommand implements CommandExecutor, TabCompleter {
	private static final String LIST_PERMISSION = "crossserver.homes.list";
	private static final String MENU_PERMISSION = "crossserver.homes.menu";
	private static final String TELEPORT_PERMISSION = "crossserver.homes.teleport";
	private static final String SET_PERMISSION = "crossserver.homes.set";
	private static final String DELETE_PERMISSION = "crossserver.homes.delete";
	private static final String DEFAULT_PERMISSION = "crossserver.homes.default";

	private final HomesSyncService homesSyncService;
	private final HomesMenuService homesMenuService;
	private final Texts texts;

	public HomesCommand(HomesSyncService homesSyncService, HomesMenuService homesMenuService, Texts texts) {
		this.homesSyncService = homesSyncService;
		this.homesMenuService = homesMenuService;
		this.texts = texts;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(texts.tr("player.only"));
			return true;
		}
		String name = command.getName().toLowerCase(Locale.ROOT);
		if ("homes".equals(name)) {
			if (!sender.hasPermission(LIST_PERMISSION) && !sender.hasPermission(MENU_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			homesMenuService.openMenu(player, 1);
			return true;
		}
		if ("sethome".equals(name)) {
			if (!sender.hasPermission(SET_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			if (args.length < 1) {
				sender.sendMessage("§eUsage: /sethome <name>");
				return true;
			}
			sender.sendMessage(homesSyncService.setHome(player, args[0]));
			return true;
		}
		if ("delhome".equals(name)) {
			if (!sender.hasPermission(DELETE_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			if (args.length < 1) {
				sender.sendMessage("§eUsage: /delhome <name>");
				return true;
			}
			sender.sendMessage(homesSyncService.deleteHome(player.getUniqueId(), args[0]));
			return true;
		}
		if ("setdefaulthome".equals(name)) {
			if (!sender.hasPermission(DEFAULT_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			if (args.length < 1) {
				sender.sendMessage("§eUsage: /setdefaulthome <name>");
				return true;
			}
			sender.sendMessage(homesSyncService.setDefaultHome(player.getUniqueId(), args[0]));
			return true;
		}
		if (!sender.hasPermission(TELEPORT_PERMISSION)) {
			sender.sendMessage(texts.tr("command.no_permission"));
			return true;
		}
		sender.sendMessage(homesSyncService.teleportHome(player, args.length >= 1 ? args[0] : null));
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		String name = command.getName().toLowerCase(Locale.ROOT);
		if (!(sender instanceof Player player)) {
			return List.of();
		}
		if (args.length == 1 && ("home".equals(name) || "delhome".equals(name) || "setdefaulthome".equals(name))) {
			return homesSyncService.listHomes(player.getUniqueId()).stream()
					.map(HomeEntry::name)
					.sorted(Comparator.naturalOrder())
					.toList();
		}
		return List.of();
	}
}
