package org.xiaoziyi.crossserver.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.economy.EconomyTransactionEntry;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class EconomyCommand implements CommandExecutor, TabCompleter {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final String BALANCE_PERMISSION = "crossserver.economy.balance";
	private static final String HISTORY_PERMISSION = "crossserver.economy.history";
	private static final String SET_PERMISSION = "crossserver.economy.set";
	private static final String DEPOSIT_PERMISSION = "crossserver.economy.deposit";
	private static final String WITHDRAW_PERMISSION = "crossserver.economy.withdraw";

	private final JavaPlugin plugin;
	private final CrossServerApi api;
	private final StorageProvider storageProvider;

	public EconomyCommand(JavaPlugin plugin, CrossServerApi api, StorageProvider storageProvider) {
		this.plugin = plugin;
		this.api = api;
		this.storageProvider = storageProvider;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {
			sender.sendMessage("§c用法: /economy <balance|set|deposit|withdraw|history> ...");
			return true;
		}

		String subCommand = args[0].toLowerCase(Locale.ROOT);
		if (!hasPermission(sender, subCommand)) {
			sender.sendMessage("§c你没有权限执行此命令。");
			return true;
		}

		if ("history".equals(subCommand)) {
			OfflinePlayer target;
			String targetPlayerArg;
			if (args.length >= 2) {
				targetPlayerArg = args[1];
			} else if (sender instanceof org.bukkit.entity.Player) {
				targetPlayerArg = sender.getName();
			} else {
				sender.sendMessage("§c控制台必须指定玩家名称。");
				sender.sendMessage("§c用法: /economy history <player> [limit]");
				return true;
			}
			target = resolvePlayer(targetPlayerArg);
			if (target == null) {
				sender.sendMessage("§c玩家 " + targetPlayerArg + " 不存在或未被服务器缓存。");
				return true;
			}
			int limit = 10;
			if (args.length >= 3) {
				try {
					limit = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					sender.sendMessage("§c无效的条数: " + args[2]);
					return true;
				}
			}
			UUID playerId = target.getUniqueId();
			String playerName = resolvePlayerName(target);
			int actualLimit = limit;
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
				try {
					List<EconomyTransactionEntry> entries = storageProvider.findEconomyTransactions(playerId, actualLimit);
					runSync(() -> {
						sender.sendMessage("§a玩家 " + playerName + " 的最近流水:");
						if (entries.isEmpty()) {
							sender.sendMessage("§7暂无流水记录");
							return;
						}
						for (EconomyTransactionEntry entry : entries) {
							sender.sendMessage("§7#" + entry.id() + " §f" + entry.transactionType() + " §e" + formatAmount(entry.amount()) + " §7-> §a" + formatAmount(entry.balanceAfter()) + " §8(" + entry.source() + ") §7" + TIME_FORMATTER.format(entry.createdAt()));
						}
					});
				} catch (Exception exception) {
					runSync(() -> sender.sendMessage("§c查询流水失败: " + exception.getMessage()));
				}
			});
			return true;
		}

		if ("balance".equals(subCommand)) {
			String balanceTarget;
			if (args.length >= 2) {
				balanceTarget = args[1];
			} else if (sender instanceof org.bukkit.entity.Player) {
				balanceTarget = sender.getName();
			} else {
				sender.sendMessage("§c控制台必须指定玩家名称。");
				return true;
			}
			OfflinePlayer balancePlayer = resolvePlayer(balanceTarget);
			if (balancePlayer == null) {
				sender.sendMessage("§c玩家 " + balanceTarget + " 不存在或未被服务器缓存。");
				return true;
			}
			UUID balanceUUID = balancePlayer.getUniqueId();
			api.getEconomyService().getBalance(balanceUUID).thenAccept(balance -> runSync(() ->
					sender.sendMessage("§a玩家 " + resolvePlayerName(balancePlayer) + " 的余额是: " + formatAmount(balance))
			));
			return true;
		}

		if (args.length < 2) {
			sender.sendMessage("§c用法: /economy " + subCommand + " <player> [amount]");
			return true;
		}

		OfflinePlayer target = resolvePlayer(args[1]);
		if (target == null) {
			sender.sendMessage("§c玩家 " + args[1] + " 不存在或未被服务器缓存。");
			return true;
		}
		UUID targetUUID = target.getUniqueId();
		String targetName = resolvePlayerName(target);
		String source = "command:" + sender.getName();

		switch (subCommand) {
			case "set":
			case "deposit":
			case "withdraw":
				if (args.length < 3) {
					sender.sendMessage("§c用法: /economy " + subCommand + " <player> <amount>");
					return true;
				}
				try {
					double amount = Double.parseDouble(args[2]);
					if (subCommand.equals("set")) {
						api.getEconomyService().setBalance(targetUUID, amount, source).thenAccept(success -> runSync(() -> {
							if (success) {
								sender.sendMessage("§a成功设置玩家 " + targetName + " 的余额为: " + formatAmount(amount));
							} else {
								sender.sendMessage("§c设置失败。");
							}
						}));
					} else if (subCommand.equals("deposit")) {
						api.getEconomyService().deposit(targetUUID, amount, source).thenAccept(success -> runSync(() -> {
							if (success) {
								sender.sendMessage("§a成功给玩家 " + targetName + " 增加: " + formatAmount(amount));
							} else {
								sender.sendMessage("§c操作失败。");
							}
						}));
					} else {
						api.getEconomyService().withdraw(targetUUID, amount, source).thenAccept(success -> runSync(() -> {
							if (success) {
								sender.sendMessage("§a成功从玩家 " + targetName + " 扣除: " + formatAmount(amount));
							} else {
								sender.sendMessage("§c余额不足。");
							}
						}));
					}
				} catch (NumberFormatException e) {
					sender.sendMessage("§c无效的金额: " + args[2]);
				}
				break;
			default:
				sender.sendMessage("§c未知的子命令: " + subCommand);
				break;
		}
		return true;
	}

	private boolean hasPermission(CommandSender sender, String subCommand) {
		return switch (subCommand) {
			case "balance" -> sender.hasPermission(BALANCE_PERMISSION);
			case "history" -> sender.hasPermission(HISTORY_PERMISSION);
			case "set" -> sender.hasPermission(SET_PERMISSION);
			case "deposit" -> sender.hasPermission(DEPOSIT_PERMISSION);
			case "withdraw" -> sender.hasPermission(WITHDRAW_PERMISSION);
			default -> false;
		};
	}

	private void runSync(Runnable runnable) {
		if (Bukkit.isPrimaryThread()) {
			runnable.run();
			return;
		}
		plugin.getServer().getScheduler().runTask(plugin, runnable);
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (args.length == 1) {
			return List.of("balance", "set", "deposit", "withdraw", "history").stream()
					.filter(subCommand -> hasPermission(sender, subCommand))
					.toList();
		}
		if (args.length == 2) {
			return null;
		}
		return Collections.emptyList();
	}

	private OfflinePlayer resolvePlayer(String input) {
		if (input == null || input.isBlank()) {
			return null;
		}
		try {
			return Bukkit.getOfflinePlayer(UUID.fromString(input));
		} catch (IllegalArgumentException ignored) {
		}
		try {
			UUID storedPlayerId = storageProvider.findPlayerIdByName(input).orElse(null);
			if (storedPlayerId != null) {
				return Bukkit.getOfflinePlayer(storedPlayerId);
			}
		} catch (Exception ignored) {
		}
		OfflinePlayer onlinePlayer = Bukkit.getPlayerExact(input);
		if (onlinePlayer != null) {
			return onlinePlayer;
		}
		OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(input);
		if (cachedPlayer == null) {
			return null;
		}
		if (!cachedPlayer.hasPlayedBefore() && !cachedPlayer.isOnline()) {
			return null;
		}
		return cachedPlayer;
	}

	private String resolvePlayerName(OfflinePlayer player) {
		if (player.getName() != null && !player.getName().isBlank()) {
			return player.getName();
		}
		try {
			return storageProvider.findPlayerName(player.getUniqueId()).orElse(player.getUniqueId().toString());
		} catch (Exception ignored) {
			return player.getUniqueId().toString();
		}
	}

	private String formatAmount(double amount) {
		return String.format(Locale.US, "%.2f", amount);
	}
}
