package org.xiaoziyi.crossserver.economy;

import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.storage.StorageProvider;
import org.xiaoziyi.crossserver.sync.SyncService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class EconomyServiceImpl implements EconomyService {
	private static final String NAMESPACE = "economy";
	private static final int SCALE = 2;
	private static final String DEFAULT_SOURCE = "api";
	private static final Executor ECONOMY_EXECUTOR = Executors.newFixedThreadPool(
			Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
			runnable -> {
				Thread thread = new Thread(runnable, "cross-server-economy");
				thread.setDaemon(true);
				return thread;
			}
	);
	private static final Map<UUID, Object> PLAYER_LOCKS = new ConcurrentHashMap<>();

	private final Logger logger;
	private final SyncService syncService;
	private final StorageProvider storageProvider;

	public EconomyServiceImpl(Logger logger, SyncService syncService, StorageProvider storageProvider) {
		this.logger = logger;
		this.syncService = syncService;
		this.storageProvider = storageProvider;
	}

	@Override
	public CompletableFuture<Double> getBalance(UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			synchronized (playerLock(playerId)) {
				try {
					Optional<PlayerSnapshot> snapshot = syncService.loadPlayerData(playerId, NAMESPACE);
					return snapshot.map(PlayerSnapshot::payload)
							.map(this::parseBalance)
							.orElse(0D);
				} catch (Exception exception) {
					throw new RuntimeException(exception);
				}
			}
		}, ECONOMY_EXECUTOR);
	}

	@Override
	public CompletableFuture<Boolean> setBalance(UUID playerId, double amount) {
		return setBalance(playerId, amount, DEFAULT_SOURCE);
	}

	@Override
	public CompletableFuture<Boolean> setBalance(UUID playerId, double amount, String source) {
		if (!isValidAmount(amount)) {
			return CompletableFuture.completedFuture(false);
		}
		double normalizedTargetBalance = normalizeAmount(amount);
		return CompletableFuture.supplyAsync(() -> {
			synchronized (playerLock(playerId)) {
				try {
					String playerName = storageProvider.findPlayerName(playerId).orElse(null);
					Optional<PlayerSnapshot> snapshot = storageProvider.updateEconomyBalance(playerId, playerName, normalizedTargetBalance, true, "set", source);
					if (snapshot.isEmpty()) {
						return false;
					}
					syncService.publishPlayerInvalidation(playerId, NAMESPACE, snapshot.get().version());
					return true;
				} catch (Exception exception) {
					throw new RuntimeException(exception);
				}
			}
		}, ECONOMY_EXECUTOR);
	}

	@Override
	public CompletableFuture<Boolean> deposit(UUID playerId, double amount) {
		return deposit(playerId, amount, DEFAULT_SOURCE);
	}

	@Override
	public CompletableFuture<Boolean> deposit(UUID playerId, double amount, String source) {
		if (!isValidAmount(amount)) {
			return CompletableFuture.completedFuture(false);
		}
		double normalizedAmount = normalizeAmount(amount);
		return CompletableFuture.supplyAsync(() -> {
			synchronized (playerLock(playerId)) {
				try {
					String playerName = storageProvider.findPlayerName(playerId).orElse(null);
					Optional<PlayerSnapshot> snapshot = storageProvider.updateEconomyBalance(playerId, playerName, normalizedAmount, false, "deposit", source);
					if (snapshot.isEmpty()) {
						return false;
					}
					syncService.publishPlayerInvalidation(playerId, NAMESPACE, snapshot.get().version());
					return true;
				} catch (Exception exception) {
					throw new RuntimeException(exception);
				}
			}
		}, ECONOMY_EXECUTOR);
	}

	@Override
	public CompletableFuture<Boolean> withdraw(UUID playerId, double amount) {
		return withdraw(playerId, amount, DEFAULT_SOURCE);
	}

	@Override
	public CompletableFuture<Boolean> withdraw(UUID playerId, double amount, String source) {
		if (!isValidAmount(amount)) {
			return CompletableFuture.completedFuture(false);
		}
		double normalizedAmount = normalizeAmount(amount);
		return CompletableFuture.supplyAsync(() -> {
			synchronized (playerLock(playerId)) {
				try {
					String playerName = storageProvider.findPlayerName(playerId).orElse(null);
					Optional<PlayerSnapshot> snapshot = storageProvider.updateEconomyBalance(playerId, playerName, normalizedAmount, false, "withdraw", source);
					if (snapshot.isEmpty()) {
						return false;
					}
					syncService.publishPlayerInvalidation(playerId, NAMESPACE, snapshot.get().version());
					return true;
				} catch (Exception exception) {
					throw new RuntimeException(exception);
				}
			}
		}, ECONOMY_EXECUTOR);
	}

	private boolean isValidAmount(double amount) {
		return Double.isFinite(amount) && amount >= 0D;
	}

	private double parseBalance(String payload) {
		try {
			return normalizeAmount(Double.parseDouble(payload));
		} catch (NumberFormatException exception) {
			return 0D;
		}
	}

	private double normalizeAmount(double amount) {
		return BigDecimal.valueOf(amount)
				.setScale(SCALE, RoundingMode.HALF_UP)
				.doubleValue();
	}

	private Object playerLock(UUID playerId) {
		return PLAYER_LOCKS.computeIfAbsent(playerId, ignored -> new Object());
	}
}
