package org.xiaoziyi.crossserver.economy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {
	CompletableFuture<Double> getBalance(UUID playerId);

	CompletableFuture<Boolean> setBalance(UUID playerId, double amount);

	CompletableFuture<Boolean> setBalance(UUID playerId, double amount, String source);

	CompletableFuture<Boolean> deposit(UUID playerId, double amount);

	CompletableFuture<Boolean> deposit(UUID playerId, double amount, String source);

	CompletableFuture<Boolean> withdraw(UUID playerId, double amount);

	CompletableFuture<Boolean> withdraw(UUID playerId, double amount, String source);
}
