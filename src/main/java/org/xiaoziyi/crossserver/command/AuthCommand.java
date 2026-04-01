package org.xiaoziyi.crossserver.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xiaoziyi.crossserver.auth.AuthService;

import java.util.List;
import java.util.Locale;

public final class AuthCommand implements org.bukkit.command.TabExecutor {
	private static final String LOGIN_PERMISSION = "crossserver.auth.login";
	private static final String REGISTER_PERMISSION = "crossserver.auth.register";
	private static final String CHANGE_PASSWORD_PERMISSION = "crossserver.auth.changepassword";

	private final AuthService authService;

	public AuthCommand(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§c该命令只能由玩家执行。");
			return true;
		}
		String name = command.getName().toLowerCase(Locale.ROOT);
		if ("login".equals(name) || "l".equals(name)) {
			if (!sender.hasPermission(LOGIN_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行此命令。");
				return true;
			}
			if (args.length < 1) {
				sender.sendMessage("§e用法: /login <password>");
				return true;
			}
			sender.sendMessage(authService.login(player, args[0]));
			return true;
		}
		if ("register".equals(name) || "reg".equals(name)) {
			if (!sender.hasPermission(REGISTER_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行此命令。");
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage("§e用法: /register <password> <confirm>");
				return true;
			}
			sender.sendMessage(authService.register(player, args[0], args[1]));
			return true;
		}
		if ("changepassword".equals(name)) {
			if (!sender.hasPermission(CHANGE_PASSWORD_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行此命令。");
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage("§e用法: /changepassword <old> <new>");
				return true;
			}
			sender.sendMessage(authService.changePassword(player, args[0], args[1]));
			return true;
		}
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		return List.of();
	}
}
