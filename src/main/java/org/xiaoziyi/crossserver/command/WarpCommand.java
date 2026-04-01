package org.xiaoziyi.crossserver.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xiaoziyi.crossserver.ui.WarpMenuService;
import org.xiaoziyi.crossserver.warp.WarpEntry;
import org.xiaoziyi.crossserver.warp.WarpService;

import java.util.List;
import java.util.Locale;

public final class WarpCommand implements TabExecutor {
	private static final String LIST_PERMISSION = "crossserver.warps.list";
	private static final String TELEPORT_PERMISSION = "crossserver.warps.teleport";
	private static final String SET_PERMISSION = "crossserver.warps.set";
	private static final String DELETE_PERMISSION = "crossserver.warps.delete";

	private final WarpService warpService;
	private final WarpMenuService warpMenuService;

	public WarpCommand(WarpService warpService, WarpMenuService warpMenuService) {
		this.warpService = warpService;
		this.warpMenuService = warpMenuService;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§c该命令只能由玩家执行。");
			return true;
		}
		String name = command.getName().toLowerCase(Locale.ROOT);
		if ("setwarp".equals(name)) {
			if (!sender.hasPermission(SET_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行此命令。");
				return true;
			}
			if (args.length < 1) {
				sender.sendMessage("§e用法: /setwarp <name>");
				return true;
			}
			sender.sendMessage(warpService.setWarp(player, args[0]));
			return true;
		}
		if ("delwarp".equals(name)) {
			if (!sender.hasPermission(DELETE_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行此命令。");
				return true;
			}
			if (args.length < 1) {
				sender.sendMessage("§e用法: /delwarp <name>");
				return true;
			}
			sender.sendMessage(warpService.deleteWarp(args[0], player.getName()));
			return true;
		}
		if (args.length < 1) {
			if (!sender.hasPermission(LIST_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行此命令。");
				return true;
			}
			warpMenuService.openMenu(player, 1);
			return true;
		}
		if (!sender.hasPermission(TELEPORT_PERMISSION)) {
			sender.sendMessage("§c你没有权限执行此命令。");
			return true;
		}
		sender.sendMessage(warpService.teleportWarp(player, args[0]));
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (args.length != 1) {
			return List.of();
		}
		String name = command.getName().toLowerCase(Locale.ROOT);
		if ("warp".equals(name) || "delwarp".equals(name)) {
			return warpService.listWarps().stream().map(WarpEntry::name).toList();
		}
		return List.of();
	}
}
