package org.xiaoziyi.crossserver.storage;

import org.xiaoziyi.crossserver.economy.EconomyTransactionEntry;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;
import org.xiaoziyi.crossserver.model.NodeStatusRecord;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.teleport.TeleportHandoffStatus;
import org.xiaoziyi.crossserver.teleport.TransferHistoryEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageProvider extends AutoCloseable {
	void start() throws Exception;

	boolean tryAcquireSession(UUID playerId, String serverId, String sessionId, Duration ttl) throws Exception;

	String prepareSessionTransfer(UUID playerId, String sessionId, String targetServerId, Duration ttl) throws Exception;

	boolean tryClaimTransferredSession(UUID playerId, String serverId, String sessionId, String transferToken, Duration ttl) throws Exception;

	Optional<org.xiaoziyi.crossserver.session.SessionTransferState> loadSessionTransferState(UUID playerId) throws Exception;

	void clearPreparedTransfer(UUID playerId, String sessionId) throws Exception;

	void clearPreparedTransfer(UUID playerId) throws Exception;

	void refreshSession(UUID playerId, String sessionId, Duration ttl) throws Exception;

	void releaseSession(UUID playerId, String sessionId) throws Exception;

	Optional<PlayerSnapshot> loadPlayerData(UUID playerId, String namespace) throws Exception;

	PlayerSnapshot savePlayerData(UUID playerId, String namespace, String payload) throws Exception;

	Optional<PlayerSnapshot> updateEconomyBalance(UUID playerId, String playerName, double amount, boolean absolute, String transactionType, String source) throws Exception;

	void savePlayerIdentity(UUID playerId, String playerName) throws Exception;

	Optional<UUID> findPlayerIdByName(String playerName) throws Exception;

	Optional<String> findPlayerName(UUID playerId) throws Exception;

	void appendEconomyTransaction(UUID playerId, String playerName, String transactionType, double amount, double balanceAfter, String source) throws Exception;

	List<EconomyTransactionEntry> findEconomyTransactions(UUID playerId, int limit) throws Exception;

	void appendTransferHistory(String requestId, UUID playerId, String playerName, String sourceServerId, String targetServerId, String eventType, TeleportHandoffStatus status, String detail) throws Exception;

	List<TransferHistoryEntry> findTransferHistory(UUID playerId, int limit) throws Exception;

	List<TransferHistoryEntry> findTransferHistoryPage(UUID playerId, int offset, int limit) throws Exception;

	List<TransferHistoryEntry> findRecentTransferHistory(int limit) throws Exception;

	List<TransferHistoryEntry> findRecentTransferHistoryPage(int offset, int limit) throws Exception;

	void upsertNodeStatus(String serverId, String cluster, Instant lastSeen, Instant updatedAt) throws Exception;

	List<NodeStatusRecord> listNodeStatuses(String cluster) throws Exception;

	Optional<GlobalSnapshot> loadGlobalData(String namespace, String dataKey) throws Exception;

	GlobalSnapshot saveGlobalData(String namespace, String dataKey, String payload) throws Exception;

	@Override
	void close() throws Exception;
}
