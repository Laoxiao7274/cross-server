package org.xiaoziyi.crossserver.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xiaoziyi.crossserver.auth.AuthService;
import org.xiaoziyi.crossserver.i18n.Texts;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class AuthCommand implements org.bukkit.command.TabExecutor {
	private static final String LOGIN_PERMISSION = "crossserver.auth.login";
	private static final String REGISTER_PERMISSION = "crossserver.auth.register";
	private static final String CHANGE_PASSWORD_PERMISSION = "crossserver.auth.changepassword";
	private static final String RESET_PASSWORD_PERMISSION = "crossserver.auth.resetpassword";

	private final AuthService authService;
	private final Texts texts;
	private final StorageProvider storageProvider;

	public AuthCommand(AuthService authService, Texts texts, StorageProvider storageProvider) {
		this.authService = authService;
		this.texts = texts;
		this.storageProvider = storageProvider;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		String name = command.getName().toLowerCase(Locale.ROOT);
		if ("resetpassword".equals(name)) {
			if (!sender.hasPermission(RESET_PASSWORD_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage(texts.tr("auth.usage.reset"));
				return true;
			}
			try {
				Optional<UUID> playerId = storageProvider.findPlayerIdByName(args[0]);
				if (playerId.isEmpty()) {
					sender.sendMessage("§c未找到玩家: " + args[0]);
					return true;
				}
				sender.sendMessage(authService.resetPassword(playerId.get(), args[1]));
			} catch (Exception exception) {
				sender.sendMessage("§c重置密码失败: " + exception.getMessage());
			}
			return true;
		}
		if (!(sender instanceof Player player)) {
			sender.sendMessage(texts.tr("player.only"));
			return true;
		}
		if ("login".equals(name) || "l".equals(name)) {
			if (!sender.hasPermission(LOGIN_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			if (args.length < 1) {
				sender.sendMessage(texts.tr("auth.usage.login"));
				return true;
			}
			sender.sendMessage(authService.login(player, args[0]));
			return true;
		}
		if ("register".equals(name) || "reg".equals(name)) {
			if (!sender.hasPermission(REGISTER_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage(texts.tr("auth.usage.register"));
				return true;
			}
			sender.sendMessage(authService.register(player, args[0], args[1]));
			return true;
		}
		if ("changepassword".equals(name)) {
			if (!sender.hasPermission(CHANGE_PASSWORD_PERMISSION)) {
				sender.sendMessage(texts.tr("command.no_permission"));
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage(texts.tr("auth.usage.change"));
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
