package org.xiaoziyi.crossserver.economy;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class VaultEconomyProvider extends AbstractEconomy {
	private static final String CURRENCY_SINGULAR = "coin";
	private static final String CURRENCY_PLURAL = "coins";
	private final Plugin plugin;
	private final EconomyService economyService;
	private final StorageProvider storageProvider;

	public VaultEconomyProvider(Plugin plugin, EconomyService economyService, StorageProvider storageProvider) {
		this.plugin = plugin;
		this.economyService = economyService;
		this.storageProvider = storageProvider;
	}

	@Override
	public boolean isEnabled() {
		return plugin.isEnabled();
	}

	@Override
	public String getName() {
		return "CrossServerEconomy";
	}

	@Override
	public boolean hasBankSupport() {
		return false;
	}

	@Override
	public int fractionalDigits() {
		return 2;
	}

	@Override
	public String format(double amount) {
		return String.format(Locale.US, "%.2f %s", amount, amount == 1D ? CURRENCY_SINGULAR : CURRENCY_PLURAL);
	}

	@Override
	public String currencyNamePlural() {
		return CURRENCY_PLURAL;
	}

	@Override
	public String currencyNameSingular() {
		return CURRENCY_SINGULAR;
	}

	@Override
	public boolean hasAccount(String playerName) {
		return resolvePlayerId(playerName) != null;
	}

	@Override
	public boolean hasAccount(OfflinePlayer player) {
		return player != null && player.getUniqueId() != null;
	}

	@Override
	public boolean hasAccount(String playerName, String worldName) {
		return hasAccount(playerName);
	}

	@Override
	public boolean hasAccount(OfflinePlayer player, String worldName) {
		return hasAccount(player);
	}

	@Override
	public double getBalance(String playerName) {
		UUID playerId = resolvePlayerId(playerName);
		return playerId == null ? 0D : getBalanceNow(playerId);
	}

	@Override
	public double getBalance(OfflinePlayer player) {
		return player == null ? 0D : getBalanceNow(player.getUniqueId());
	}

	@Override
	public double getBalance(String playerName, String world) {
		return getBalance(playerName);
	}

	@Override
	public double getBalance(OfflinePlayer player, String world) {
		return getBalance(player);
	}

	@Override
	public boolean has(String playerName, double amount) {
		return getBalance(playerName) >= amount;
	}

	@Override
	public boolean has(OfflinePlayer player, double amount) {
		return getBalance(player) >= amount;
	}

	@Override
	public boolean has(String playerName, String worldName, double amount) {
		return has(playerName, amount);
	}

	@Override
	public boolean has(OfflinePlayer player, String worldName, double amount) {
		return has(player, amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(String playerName, double amount) {
		UUID playerId = resolvePlayerId(playerName);
		return withdraw(playerId, amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
		return withdraw(player == null ? null : player.getUniqueId(), amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
		return withdrawPlayer(playerName, amount);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
		return withdrawPlayer(player, amount);
	}

	@Override
	public EconomyResponse depositPlayer(String playerName, double amount) {
		UUID playerId = resolvePlayerId(playerName);
		return deposit(playerId, amount);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
		return deposit(player == null ? null : player.getUniqueId(), amount);
	}

	@Override
	public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
		return depositPlayer(playerName, amount);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
		return depositPlayer(player, amount);
	}

	@Override
	public EconomyResponse createBank(String name, String player) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse createBank(String name, OfflinePlayer player) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse deleteBank(String name) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse bankBalance(String name) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse bankHas(String name, double amount) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse bankWithdraw(String name, double amount) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse bankDeposit(String name, double amount) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse isBankOwner(String name, String playerName) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse isBankMember(String name, String playerName) {
		return bankUnsupported();
	}

	@Override
	public EconomyResponse isBankMember(String name, OfflinePlayer player) {
		return bankUnsupported();
	}

	@Override
	public List<String> getBanks() {
		return List.of();
	}

	@Override
	public boolean createPlayerAccount(String playerName) {
		UUID playerId = resolvePlayerId(playerName);
		return createAccount(playerId);
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player) {
		return createAccount(player == null ? null : player.getUniqueId());
	}

	@Override
	public boolean createPlayerAccount(String playerName, String worldName) {
		return createPlayerAccount(playerName);
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
		return createPlayerAccount(player);
	}

	private UUID resolvePlayerId(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(playerName);
		} catch (IllegalArgumentException ignored) {
		}
		try {
			UUID storedPlayerId = storageProvider.findPlayerIdByName(playerName).orElse(null);
			if (storedPlayerId != null) {
				return storedPlayerId;
			}
		} catch (Exception ignored) {
		}
		OfflinePlayer onlinePlayer = Bukkit.getPlayerExact(playerName);
		if (onlinePlayer != null) {
			rememberPlayerIdentity(onlinePlayer);
			return onlinePlayer.getUniqueId();
		}
		OfflinePlayer cachedPlayer = Bukkit.getOfflinePlayerIfCached(playerName);
		if (cachedPlayer == null) {
			return null;
		}
		if (!cachedPlayer.hasPlayedBefore() && !cachedPlayer.isOnline()) {
			return null;
		}
		rememberPlayerIdentity(cachedPlayer);
		return cachedPlayer.getUniqueId();
	}

	private double getBalanceNow(UUID playerId) {
		if (playerId == null) {
			return 0D;
		}
		try {
			return economyService.getBalance(playerId).join();
		} catch (Exception exception) {
			return 0D;
		}
	}

	private boolean createAccount(UUID playerId) {
		if (playerId == null) {
			return false;
		}
		try {
			return economyService.setBalance(playerId, getBalanceNow(playerId), "vault:create-account").join();
		} catch (Exception exception) {
			return false;
		}
	}

	private EconomyResponse deposit(UUID playerId, double amount) {
		if (playerId == null) {
			return failure(amount, "玩家不存在");
		}
		if (!Double.isFinite(amount) || amount < 0D) {
			return failure(amount, "金额无效");
		}
		try {
			boolean success = economyService.deposit(playerId, amount, "vault:deposit").join();
			double balance = getBalanceNow(playerId);
			return success ? success(amount, balance) : failure(amount, "存款失败");
		} catch (Exception exception) {
			return failure(amount, exception.getMessage());
		}
	}

	private EconomyResponse withdraw(UUID playerId, double amount) {
		if (playerId == null) {
			return failure(amount, "玩家不存在");
		}
		if (!Double.isFinite(amount) || amount < 0D) {
			return failure(amount, "金额无效");
		}
		double balance = getBalanceNow(playerId);
		if (balance < amount) {
			return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "余额不足");
		}
		try {
			boolean success = economyService.withdraw(playerId, amount, "vault:withdraw").join();
			double newBalance = getBalanceNow(playerId);
			return success ? success(amount, newBalance) : failure(amount, "取款失败");
		} catch (Exception exception) {
			return failure(amount, exception.getMessage());
		}
	}

	private void rememberPlayerIdentity(OfflinePlayer player) {
		if (player == null || player.getUniqueId() == null || player.getName() == null || player.getName().isBlank()) {
			return;
		}
		try {
			storageProvider.savePlayerIdentity(player.getUniqueId(), player.getName());
		} catch (Exception ignored) {
		}
	}

	private EconomyResponse bankUnsupported() {
		return new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "不支持银行功能");
	}

	private EconomyResponse success(double amount, double balance) {
		return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
	}

	private EconomyResponse failure(double amount, String errorMessage) {
		return new EconomyResponse(amount, 0D, EconomyResponse.ResponseType.FAILURE, errorMessage);
	}
}
