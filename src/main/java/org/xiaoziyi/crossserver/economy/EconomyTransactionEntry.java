package org.xiaoziyi.crossserver.economy;

import java.time.Instant;
import java.util.UUID;

public record EconomyTransactionEntry(
		long id,
		UUID playerId,
		String playerName,
		String transactionType,
		double amount,
		double balanceAfter,
		String source,
		Instant createdAt
) {
}
