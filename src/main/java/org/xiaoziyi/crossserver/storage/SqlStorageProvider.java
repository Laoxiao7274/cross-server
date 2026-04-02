package org.xiaoziyi.crossserver.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.economy.EconomyTransactionEntry;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;
import org.xiaoziyi.crossserver.model.NodeStatusRecord;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.session.SessionTransferState;
import org.xiaoziyi.crossserver.teleport.TeleportHandoffStatus;
import org.xiaoziyi.crossserver.teleport.TransferHistoryEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class SqlStorageProvider implements StorageProvider {
	private static final String CREATE_PLAYER_SESSION_TABLE = """
				CREATE TABLE IF NOT EXISTS player_session (
					player_uuid VARCHAR(36) PRIMARY KEY,
					current_server VARCHAR(64) NOT NULL,
					session_id VARCHAR(64) NOT NULL,
					lock_until TIMESTAMP NOT NULL,
					last_seen TIMESTAMP NOT NULL,
					transfer_token VARCHAR(64) NULL,
					transfer_target_server VARCHAR(64) NULL,
					transfer_prepared_at TIMESTAMP NULL,
					transfer_expires_at TIMESTAMP NULL
				)
				""";
	private static final String CREATE_PLAYER_SNAPSHOT_TABLE = """
				CREATE TABLE IF NOT EXISTS player_snapshot (
					player_uuid VARCHAR(36) NOT NULL,
					namespace VARCHAR(120) NOT NULL,
					payload LONGTEXT NOT NULL,
					version BIGINT NOT NULL,
					updated_at TIMESTAMP NOT NULL,
					PRIMARY KEY (player_uuid, namespace)
				)
				""";
	private static final String CREATE_GLOBAL_SNAPSHOT_TABLE = """
				CREATE TABLE IF NOT EXISTS global_snapshot (
					namespace VARCHAR(120) NOT NULL,
					data_key VARCHAR(120) NOT NULL,
					payload LONGTEXT NOT NULL,
					version BIGINT NOT NULL,
					updated_at TIMESTAMP NOT NULL,
					PRIMARY KEY (namespace, data_key)
				)
				""";
	private static final String CREATE_PLAYER_IDENTITY_TABLE = """
				CREATE TABLE IF NOT EXISTS player_identity (
					player_uuid VARCHAR(36) NOT NULL,
					player_name VARCHAR(32) NOT NULL,
					player_name_normalized VARCHAR(32) NOT NULL,
					updated_at TIMESTAMP NOT NULL,
					PRIMARY KEY (player_uuid),
					UNIQUE KEY uk_player_identity_name (player_name_normalized)
				)
				""";
	private static final String CREATE_ECONOMY_TRANSACTION_TABLE = """
				CREATE TABLE IF NOT EXISTS economy_transaction (
					id BIGINT NOT NULL AUTO_INCREMENT,
					player_uuid VARCHAR(36) NOT NULL,
					player_name VARCHAR(32) NULL,
					transaction_type VARCHAR(24) NOT NULL,
					amount DECIMAL(19, 2) NOT NULL,
					balance_after DECIMAL(19, 2) NOT NULL,
					source VARCHAR(64) NOT NULL,
					created_at TIMESTAMP NOT NULL,
					PRIMARY KEY (id),
					KEY idx_economy_transaction_player (player_uuid),
					KEY idx_economy_transaction_created_at (created_at)
				)
				""";
	private static final String CREATE_TRANSFER_HISTORY_TABLE = """
				CREATE TABLE IF NOT EXISTS transfer_history (
					id BIGINT NOT NULL AUTO_INCREMENT,
					request_id VARCHAR(64) NOT NULL,
					player_uuid VARCHAR(36) NOT NULL,
					player_name VARCHAR(32) NULL,
					source_server_id VARCHAR(64) NULL,
					target_server_id VARCHAR(64) NULL,
					event_type VARCHAR(32) NOT NULL,
					status VARCHAR(24) NOT NULL,
					detail VARCHAR(255) NULL,
					created_at TIMESTAMP NOT NULL,
					PRIMARY KEY (id),
					KEY idx_transfer_history_player (player_uuid),
					KEY idx_transfer_history_request (request_id),
					KEY idx_transfer_history_created_at (created_at)
				)
				""";
	private static final String CREATE_NODE_STATUS_TABLE = """
				CREATE TABLE IF NOT EXISTS node_status (
					server_id VARCHAR(64) PRIMARY KEY,
					cluster VARCHAR(64) NOT NULL,
					last_seen TIMESTAMP NOT NULL,
					updated_at TIMESTAMP NOT NULL
				)
				""";
	private static final String ECONOMY_NAMESPACE = "economy";

	private final Logger logger;
	private final PluginConfiguration.DatabaseSettings settings;
	private HikariDataSource dataSource;

	public SqlStorageProvider(Logger logger, PluginConfiguration.DatabaseSettings settings) {
		this.logger = logger;
		this.settings = settings;
	}

	@Override
	public void start() throws Exception {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(settings.jdbcUrl());
		config.setUsername(settings.username());
		config.setPassword(settings.password());
		config.setMaximumPoolSize(settings.maximumPoolSize());
		config.setPoolName("cross-server-pool");
		this.dataSource = new HikariDataSource(config);
		initializeSchema();
		logger.info("数据库连接已初始化");
	}

	@Override
	public boolean tryAcquireSession(UUID playerId, String serverId, String sessionId, Duration ttl) throws Exception {
		Instant now = Instant.now();
		Instant lockUntil = now.plus(ttl);
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try {
				String selectSql = "SELECT session_id, lock_until FROM player_session WHERE player_uuid = ? FOR UPDATE";
				try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
					statement.setString(1, playerId.toString());
					try (ResultSet resultSet = statement.executeQuery()) {
						if (!resultSet.next()) {
							insertSession(connection, playerId, serverId, sessionId, now, lockUntil);
							connection.commit();
							return true;
						}
						Instant currentLockUntil = resultSet.getTimestamp("lock_until").toInstant();
						String currentSessionId = resultSet.getString("session_id");
						if (currentLockUntil.isAfter(now) && !currentSessionId.equals(sessionId)) {
							connection.rollback();
							return false;
						}
					}
				}
				updateSession(connection, playerId, serverId, sessionId, now, lockUntil);
				connection.commit();
				return true;
			} catch (Exception exception) {
				connection.rollback();
				throw exception;
			} finally {
				connection.setAutoCommit(true);
			}
		}
	}

	@Override
	public String prepareSessionTransfer(UUID playerId, String sessionId, String targetServerId, Duration ttl) throws Exception {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(ttl);
		String transferToken = UUID.randomUUID().toString();
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try {
				String selectSql = "SELECT session_id, lock_until FROM player_session WHERE player_uuid = ? FOR UPDATE";
				try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
					statement.setString(1, playerId.toString());
					try (ResultSet resultSet = statement.executeQuery()) {
						if (!resultSet.next()) {
							connection.rollback();
							throw new IllegalStateException("玩家当前没有可接力的会话");
						}
						String currentSessionId = resultSet.getString("session_id");
						Instant currentLockUntil = resultSet.getTimestamp("lock_until").toInstant();
						if (!sessionId.equals(currentSessionId) || !currentLockUntil.isAfter(now)) {
							connection.rollback();
							throw new IllegalStateException("玩家当前会话已失效，无法准备接力");
						}
					}
				}
				String updateSql = """
						UPDATE player_session
						SET transfer_token = ?, transfer_target_server = ?, transfer_prepared_at = ?, transfer_expires_at = ?
						WHERE player_uuid = ? AND session_id = ?
						""";
				try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
					statement.setString(1, transferToken);
					statement.setString(2, targetServerId);
					statement.setTimestamp(3, Timestamp.from(now));
					statement.setTimestamp(4, Timestamp.from(expiresAt));
					statement.setString(5, playerId.toString());
					statement.setString(6, sessionId);
					if (statement.executeUpdate() != 1) {
						connection.rollback();
						throw new IllegalStateException("准备会话接力失败");
					}
				}
				connection.commit();
				return transferToken;
			} catch (Exception exception) {
				connection.rollback();
				throw exception;
			} finally {
				connection.setAutoCommit(true);
			}
		}
	}

	@Override
	public boolean tryClaimTransferredSession(UUID playerId, String serverId, String sessionId, String transferToken, Duration ttl) throws Exception {
		Instant now = Instant.now();
		Instant lockUntil = now.plus(ttl);
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try {
				String selectSql = "SELECT lock_until, transfer_token, transfer_target_server, transfer_expires_at FROM player_session WHERE player_uuid = ? FOR UPDATE";
				try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
					statement.setString(1, playerId.toString());
					try (ResultSet resultSet = statement.executeQuery()) {
						if (!resultSet.next()) {
							connection.rollback();
							return false;
						}
						Timestamp transferExpiresAtValue = resultSet.getTimestamp("transfer_expires_at");
						String currentTransferToken = resultSet.getString("transfer_token");
						String transferTargetServer = resultSet.getString("transfer_target_server");
						if (currentTransferToken == null || transferExpiresAtValue == null || !transferToken.equals(currentTransferToken) || !serverId.equalsIgnoreCase(transferTargetServer) || !transferExpiresAtValue.toInstant().isAfter(now)) {
							connection.rollback();
							return false;
						}
					}
				}
				String updateSql = """
						UPDATE player_session
						SET current_server = ?, session_id = ?, lock_until = ?, last_seen = ?,
							transfer_token = NULL, transfer_target_server = NULL, transfer_prepared_at = NULL, transfer_expires_at = NULL
						WHERE player_uuid = ? AND transfer_token = ? AND transfer_target_server = ?
						""";
				try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
					statement.setString(1, serverId);
					statement.setString(2, sessionId);
					statement.setTimestamp(3, Timestamp.from(lockUntil));
					statement.setTimestamp(4, Timestamp.from(now));
					statement.setString(5, playerId.toString());
					statement.setString(6, transferToken);
					statement.setString(7, serverId);
					if (statement.executeUpdate() != 1) {
						connection.rollback();
						return false;
					}
				}
				connection.commit();
				return true;
			} catch (Exception exception) {
				connection.rollback();
				throw exception;
			} finally {
				connection.setAutoCommit(true);
			}
		}
	}

	@Override
	public Optional<SessionTransferState> loadSessionTransferState(UUID playerId) throws Exception {
		String sql = """
				SELECT current_server, session_id, lock_until, transfer_token, transfer_target_server, transfer_prepared_at, transfer_expires_at
				FROM player_session
				WHERE player_uuid = ?
				""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				return Optional.of(new SessionTransferState(
						resultSet.getString("current_server"),
						resultSet.getString("session_id"),
						toInstant(resultSet.getTimestamp("lock_until")),
						resultSet.getString("transfer_token"),
						resultSet.getString("transfer_target_server"),
						toInstant(resultSet.getTimestamp("transfer_prepared_at")),
						toInstant(resultSet.getTimestamp("transfer_expires_at"))
				));
			}
		}
	}

	@Override
	public void clearPreparedTransfer(UUID playerId, String sessionId) throws Exception {
		String sql = """
				UPDATE player_session
				SET transfer_token = NULL, transfer_target_server = NULL, transfer_prepared_at = NULL, transfer_expires_at = NULL
				WHERE player_uuid = ? AND session_id = ?
				""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setString(2, sessionId);
			statement.executeUpdate();
		}
	}

	@Override
	public void clearPreparedTransfer(UUID playerId) throws Exception {
		String sql = """
				UPDATE player_session
				SET transfer_token = NULL, transfer_target_server = NULL, transfer_prepared_at = NULL, transfer_expires_at = NULL
				WHERE player_uuid = ?
				""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.executeUpdate();
		}
	}

	@Override
	public void refreshSession(UUID playerId, String sessionId, Duration ttl) throws Exception {
		String sql = """
					UPDATE player_session
					SET lock_until = ?, last_seen = ?
					WHERE player_uuid = ? AND session_id = ?
					""";
		Instant now = Instant.now();
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setTimestamp(1, Timestamp.from(now.plus(ttl)));
			statement.setTimestamp(2, Timestamp.from(now));
			statement.setString(3, playerId.toString());
			statement.setString(4, sessionId);
			statement.executeUpdate();
		}
	}

	@Override
	public void releaseSession(UUID playerId, String sessionId) throws Exception {
		String sql = "DELETE FROM player_session WHERE player_uuid = ? AND session_id = ?";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setString(2, sessionId);
			statement.executeUpdate();
		}
	}

	@Override
	public Optional<PlayerSnapshot> loadPlayerData(UUID playerId, String namespace) throws Exception {
		String sql = """
					SELECT payload, version, updated_at
					FROM player_snapshot
					WHERE player_uuid = ? AND namespace = ?
					""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setString(2, namespace);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				return Optional.of(new PlayerSnapshot(
						playerId,
						namespace,
						resultSet.getString("payload"),
						resultSet.getLong("version"),
						resultSet.getTimestamp("updated_at").toInstant()
				));
			}
		}
	}

	@Override
	public PlayerSnapshot savePlayerData(UUID playerId, String namespace, String payload) throws Exception {
		Instant now = Instant.now();
		long nextVersion = 1L;
		String selectSql = """
				SELECT version
				FROM player_snapshot
				WHERE player_uuid = ? AND namespace = ?
				""";
		String saveSql = """
				INSERT INTO player_snapshot (player_uuid, namespace, payload, version, updated_at)
				VALUES (?, ?, ?, 1, ?)
				ON DUPLICATE KEY UPDATE
					payload = VALUES(payload),
					version = version + 1,
					updated_at = VALUES(updated_at)
				""";
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement select = connection.prepareStatement(selectSql)) {
				select.setString(1, playerId.toString());
				select.setString(2, namespace);
				try (ResultSet resultSet = select.executeQuery()) {
					if (resultSet.next()) {
						nextVersion = resultSet.getLong("version") + 1L;
					}
				}
			}
			try (PreparedStatement statement = connection.prepareStatement(saveSql)) {
				statement.setString(1, playerId.toString());
				statement.setString(2, namespace);
				statement.setString(3, payload);
				statement.setTimestamp(4, Timestamp.from(now));
				statement.executeUpdate();
			}
		}
		return new PlayerSnapshot(playerId, namespace, payload, nextVersion, now);
	}

	@Override
	public void deletePlayerData(UUID playerId, String namespace) throws Exception {
		String sql = "DELETE FROM player_snapshot WHERE player_uuid = ? AND namespace = ?";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setString(2, namespace);
			statement.executeUpdate();
		}
	}

	@Override
	public Optional<PlayerSnapshot> updateEconomyBalance(UUID playerId, String playerName, double amount, boolean absolute, String transactionType, String source) throws Exception {
		BigDecimal normalizedAmount = normalizeAmount(amount);
		String normalizedPlayerName = normalizePlayerName(playerName);
		String normalizedSource = normalizeSource(source);
		for (int attempt = 0; attempt < 2; attempt++) {
			Instant now = Instant.now();
			try (Connection connection = dataSource.getConnection()) {
				connection.setAutoCommit(false);
				try {
					BigDecimal currentBalance = BigDecimal.ZERO;
					long currentVersion = 0L;
					boolean existed = false;
					String selectSql = """
							SELECT payload, version
							FROM player_snapshot
							WHERE player_uuid = ? AND namespace = ?
							FOR UPDATE
							""";
					try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
						statement.setString(1, playerId.toString());
						statement.setString(2, ECONOMY_NAMESPACE);
						try (ResultSet resultSet = statement.executeQuery()) {
							if (resultSet.next()) {
								existed = true;
								currentBalance = parseBalance(resultSet.getString("payload"));
								currentVersion = resultSet.getLong("version");
							}
						}
					}

					BigDecimal newBalance = absolute ? normalizedAmount : currentBalance.add(normalizedAmount);
					if (!absolute && "withdraw".equalsIgnoreCase(transactionType) && currentBalance.compareTo(normalizedAmount) < 0) {
						connection.rollback();
						return Optional.empty();
					}
					String payload = formatBalance(newBalance);
					long nextVersion = existed ? currentVersion + 1 : 1L;

					if (existed) {
						String updateSql = """
								UPDATE player_snapshot
								SET payload = ?, version = version + 1, updated_at = ?
								WHERE player_uuid = ? AND namespace = ?
								""";
						try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
							statement.setString(1, payload);
							statement.setTimestamp(2, Timestamp.from(now));
							statement.setString(3, playerId.toString());
							statement.setString(4, ECONOMY_NAMESPACE);
							statement.executeUpdate();
						}
					} else {
						String insertSql = """
								INSERT INTO player_snapshot (player_uuid, namespace, payload, version, updated_at)
								VALUES (?, ?, ?, 1, ?)
								""";
						try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
							statement.setString(1, playerId.toString());
							statement.setString(2, ECONOMY_NAMESPACE);
							statement.setString(3, payload);
							statement.setTimestamp(4, Timestamp.from(now));
							statement.executeUpdate();
						} catch (SQLIntegrityConstraintViolationException duplicate) {
							connection.rollback();
							continue;
						}
					}

					appendEconomyTransaction(connection, playerId, normalizedPlayerName, transactionType, normalizedAmount, newBalance, normalizedSource, now);
					connection.commit();
					return Optional.of(new PlayerSnapshot(playerId, ECONOMY_NAMESPACE, payload, nextVersion, now));
				} catch (Exception exception) {
					connection.rollback();
					throw exception;
				} finally {
					connection.setAutoCommit(true);
				}
			}
		}
		throw new IllegalStateException("更新经济余额失败，重试次数已耗尽");
	}

	@Override
	public void savePlayerIdentity(UUID playerId, String playerName) throws Exception {
		String normalizedName = normalizePlayerName(playerName);
		if (normalizedName == null) {
			return;
		}
		String sql = """
					INSERT INTO player_identity (player_uuid, player_name, player_name_normalized, updated_at)
					VALUES (?, ?, ?, ?)
					ON DUPLICATE KEY UPDATE
						player_uuid = VALUES(player_uuid),
						player_name = VALUES(player_name),
						player_name_normalized = VALUES(player_name_normalized),
						updated_at = VALUES(updated_at)
					""";
		Instant now = Instant.now();
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setString(2, playerName);
			statement.setString(3, normalizedName);
			statement.setTimestamp(4, Timestamp.from(now));
			statement.executeUpdate();
		}
	}

	@Override
	public Optional<UUID> findPlayerIdByName(String playerName) throws Exception {
		String normalizedName = normalizePlayerName(playerName);
		if (normalizedName == null) {
			return Optional.empty();
		}
		String sql = """
					SELECT player_uuid
					FROM player_identity
					WHERE player_name_normalized = ?
					""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, normalizedName);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				return Optional.of(UUID.fromString(resultSet.getString("player_uuid")));
			}
		}
	}

	@Override
	public Optional<String> findPlayerName(UUID playerId) throws Exception {
		String sql = """
					SELECT player_name
					FROM player_identity
					WHERE player_uuid = ?
					""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				return Optional.ofNullable(resultSet.getString("player_name"));
			}
		}
	}

	@Override
	public void appendEconomyTransaction(UUID playerId, String playerName, String transactionType, double amount, double balanceAfter, String source) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			appendEconomyTransaction(connection, playerId, normalizePlayerName(playerName), transactionType, normalizeAmount(amount), normalizeAmount(balanceAfter), normalizeSource(source), Instant.now());
		}
	}

	@Override
	public List<EconomyTransactionEntry> findEconomyTransactions(UUID playerId, int limit) throws Exception {
		int actualLimit = Math.max(1, Math.min(limit, 100));
		String sql = """
					SELECT id, player_uuid, player_name, transaction_type, amount, balance_after, source, created_at
					FROM economy_transaction
					WHERE player_uuid = ?
					ORDER BY id DESC
					LIMIT ?
					""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setInt(2, actualLimit);
			try (ResultSet resultSet = statement.executeQuery()) {
				List<EconomyTransactionEntry> entries = new ArrayList<>();
				while (resultSet.next()) {
					entries.add(new EconomyTransactionEntry(
							resultSet.getLong("id"),
							UUID.fromString(resultSet.getString("player_uuid")),
							resultSet.getString("player_name"),
							resultSet.getString("transaction_type"),
							resultSet.getDouble("amount"),
							resultSet.getDouble("balance_after"),
							resultSet.getString("source"),
							resultSet.getTimestamp("created_at").toInstant()
					));
				}
				return entries;
			}
		}
	}

	@Override
	public void appendTransferHistory(String requestId, UUID playerId, String playerName, String sourceServerId, String targetServerId, String eventType, TeleportHandoffStatus status, String detail) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			appendTransferHistory(connection, requestId, playerId, normalizePlayerName(playerName), normalizeServerId(sourceServerId), normalizeServerId(targetServerId), normalizeEventType(eventType), status, normalizeDetail(detail), Instant.now());
		}
	}

	@Override
	public List<TransferHistoryEntry> findTransferHistory(UUID playerId, int limit) throws Exception {
		return findTransferHistoryPage(playerId, 0, limit);
	}

	@Override
	public List<TransferHistoryEntry> findTransferHistoryPage(UUID playerId, int offset, int limit) throws Exception {
		int actualOffset = Math.max(0, offset);
		int actualLimit = Math.max(1, Math.min(limit, 100));
		String sql = """
				SELECT id, request_id, player_uuid, player_name, source_server_id, target_server_id, event_type, status, detail, created_at
				FROM transfer_history
				WHERE player_uuid = ?
				ORDER BY id DESC
				LIMIT ? OFFSET ?
				""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setInt(2, actualLimit);
			statement.setInt(3, actualOffset);
			try (ResultSet resultSet = statement.executeQuery()) {
				List<TransferHistoryEntry> entries = new ArrayList<>();
				while (resultSet.next()) {
					entries.add(mapTransferHistoryEntry(resultSet));
				}
				return entries;
			}
		}
	}

	@Override
	public List<TransferHistoryEntry> findRecentTransferHistory(int limit) throws Exception {
		return findRecentTransferHistoryPage(0, limit);
	}

	@Override
	public List<TransferHistoryEntry> findRecentTransferHistoryPage(int offset, int limit) throws Exception {
		int actualOffset = Math.max(0, offset);
		int actualLimit = Math.max(1, Math.min(limit, 100));
		String sql = """
				SELECT id, request_id, player_uuid, player_name, source_server_id, target_server_id, event_type, status, detail, created_at
				FROM transfer_history
				ORDER BY id DESC
				LIMIT ? OFFSET ?
				""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, actualLimit);
			statement.setInt(2, actualOffset);
			try (ResultSet resultSet = statement.executeQuery()) {
				List<TransferHistoryEntry> entries = new ArrayList<>();
				while (resultSet.next()) {
					entries.add(mapTransferHistoryEntry(resultSet));
				}
				return entries;
			}
		}
	}

	@Override
	public void upsertNodeStatus(String serverId, String cluster, Instant lastSeen, Instant updatedAt) throws Exception {
		String sql = """
					INSERT INTO node_status (server_id, cluster, last_seen, updated_at)
					VALUES (?, ?, ?, ?)
					ON DUPLICATE KEY UPDATE
						cluster = VALUES(cluster),
						last_seen = VALUES(last_seen),
						updated_at = VALUES(updated_at)
					""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, serverId);
			statement.setString(2, cluster);
			statement.setTimestamp(3, Timestamp.from(lastSeen));
			statement.setTimestamp(4, Timestamp.from(updatedAt));
			statement.executeUpdate();
		}
	}

	@Override
	public List<NodeStatusRecord> listNodeStatuses(String cluster) throws Exception {
		String sql = """
					SELECT server_id, cluster, last_seen, updated_at
					FROM node_status
					WHERE cluster = ?
					ORDER BY updated_at DESC
					""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, cluster);
			try (ResultSet resultSet = statement.executeQuery()) {
				List<NodeStatusRecord> records = new ArrayList<>();
				while (resultSet.next()) {
					Instant lastSeen = resultSet.getTimestamp("last_seen").toInstant();
					Instant updatedAt = resultSet.getTimestamp("updated_at").toInstant();
					long latencyMillis = Math.max(0L, updatedAt.toEpochMilli() - lastSeen.toEpochMilli());
					records.add(new NodeStatusRecord(
							resultSet.getString("server_id"),
							resultSet.getString("cluster"),
							"unknown",
							latencyMillis,
							lastSeen,
							updatedAt
					));
				}
				return records;
			}
		}
	}

	@Override
	public Optional<GlobalSnapshot> loadGlobalData(String namespace, String dataKey) throws Exception {
		String sql = """
					SELECT payload, version, updated_at
					FROM global_snapshot
					WHERE namespace = ? AND data_key = ?
					""";
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, namespace);
			statement.setString(2, dataKey);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				return Optional.of(new GlobalSnapshot(
						namespace,
						dataKey,
						resultSet.getString("payload"),
						resultSet.getLong("version"),
						resultSet.getTimestamp("updated_at").toInstant()
				));
			}
		}
	}

	@Override
	public GlobalSnapshot saveGlobalData(String namespace, String dataKey, String payload) throws Exception {
		Instant now = Instant.now();
		long nextVersion = 1L;
		String selectSql = """
				SELECT version
				FROM global_snapshot
				WHERE namespace = ? AND data_key = ?
				""";
		String saveSql = """
				INSERT INTO global_snapshot (namespace, data_key, payload, version, updated_at)
				VALUES (?, ?, ?, 1, ?)
				ON DUPLICATE KEY UPDATE
					payload = VALUES(payload),
					version = version + 1,
					updated_at = VALUES(updated_at)
				""";
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement select = connection.prepareStatement(selectSql)) {
				select.setString(1, namespace);
				select.setString(2, dataKey);
				try (ResultSet resultSet = select.executeQuery()) {
					if (resultSet.next()) {
						nextVersion = resultSet.getLong("version") + 1L;
					}
				}
			}
			try (PreparedStatement statement = connection.prepareStatement(saveSql)) {
				statement.setString(1, namespace);
				statement.setString(2, dataKey);
				statement.setString(3, payload);
				statement.setTimestamp(4, Timestamp.from(now));
				statement.executeUpdate();
			}
		}
		return new GlobalSnapshot(namespace, dataKey, payload, nextVersion, now);
	}

	@Override
	public void close() {
		if (dataSource != null) {
			dataSource.close();
		}
	}

	private void initializeSchema() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement playerSession = connection.prepareStatement(CREATE_PLAYER_SESSION_TABLE)) {
				playerSession.execute();
			}
			ensurePlayerSessionColumn(connection, "transfer_token", "ALTER TABLE player_session ADD COLUMN transfer_token VARCHAR(64) NULL");
			ensurePlayerSessionColumn(connection, "transfer_target_server", "ALTER TABLE player_session ADD COLUMN transfer_target_server VARCHAR(64) NULL");
			ensurePlayerSessionColumn(connection, "transfer_prepared_at", "ALTER TABLE player_session ADD COLUMN transfer_prepared_at TIMESTAMP NULL");
			ensurePlayerSessionColumn(connection, "transfer_expires_at", "ALTER TABLE player_session ADD COLUMN transfer_expires_at TIMESTAMP NULL");
			try (PreparedStatement playerSnapshot = connection.prepareStatement(CREATE_PLAYER_SNAPSHOT_TABLE)) {
				playerSnapshot.execute();
			}
			try (PreparedStatement globalSnapshot = connection.prepareStatement(CREATE_GLOBAL_SNAPSHOT_TABLE)) {
				globalSnapshot.execute();
			}
			try (PreparedStatement playerIdentity = connection.prepareStatement(CREATE_PLAYER_IDENTITY_TABLE)) {
				playerIdentity.execute();
			}
			try (PreparedStatement economyTransaction = connection.prepareStatement(CREATE_ECONOMY_TRANSACTION_TABLE)) {
				economyTransaction.execute();
			}
			try (PreparedStatement transferHistory = connection.prepareStatement(CREATE_TRANSFER_HISTORY_TABLE)) {
				transferHistory.execute();
			}
			try (PreparedStatement nodeStatus = connection.prepareStatement(CREATE_NODE_STATUS_TABLE)) {
				nodeStatus.execute();
			}
		}
	}

	private void insertSession(Connection connection, UUID playerId, String serverId, String sessionId, Instant now, Instant lockUntil) throws Exception {
		String sql = """
					INSERT INTO player_session (player_uuid, current_server, session_id, lock_until, last_seen)
					VALUES (?, ?, ?, ?, ?)
					""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setString(2, serverId);
			statement.setString(3, sessionId);
			statement.setTimestamp(4, Timestamp.from(lockUntil));
			statement.setTimestamp(5, Timestamp.from(now));
			statement.executeUpdate();
		}
	}

	private void updateSession(Connection connection, UUID playerId, String serverId, String sessionId, Instant now, Instant lockUntil) throws Exception {
		String sql = """
					UPDATE player_session
					SET current_server = ?, session_id = ?, lock_until = ?, last_seen = ?,
						transfer_token = NULL, transfer_target_server = NULL, transfer_prepared_at = NULL, transfer_expires_at = NULL
					WHERE player_uuid = ?
					""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, serverId);
			statement.setString(2, sessionId);
			statement.setTimestamp(3, Timestamp.from(lockUntil));
			statement.setTimestamp(4, Timestamp.from(now));
			statement.setString(5, playerId.toString());
			statement.executeUpdate();
		}
	}

	private void ensurePlayerSessionColumn(Connection connection, String columnName, String alterSql) throws Exception {
		String sql = "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'player_session' AND column_name = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, columnName);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return;
				}
			}
		}
		try (PreparedStatement statement = connection.prepareStatement(alterSql)) {
			statement.execute();
		}
	}

	private void appendEconomyTransaction(Connection connection, UUID playerId, String playerName, String transactionType, BigDecimal amount, BigDecimal balanceAfter, String source, Instant createdAt) throws Exception {
		String sql = """
					INSERT INTO economy_transaction (player_uuid, player_name, transaction_type, amount, balance_after, source, created_at)
					VALUES (?, ?, ?, ?, ?, ?, ?)
					""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, playerId.toString());
			statement.setString(2, playerName);
			statement.setString(3, transactionType);
			statement.setBigDecimal(4, amount);
			statement.setBigDecimal(5, balanceAfter);
			statement.setString(6, source);
			statement.setTimestamp(7, Timestamp.from(createdAt));
			statement.executeUpdate();
		}
	}

	private void appendTransferHistory(Connection connection, String requestId, UUID playerId, String playerName, String sourceServerId, String targetServerId, String eventType, TeleportHandoffStatus status, String detail, Instant createdAt) throws Exception {
		String sql = """
					INSERT INTO transfer_history (request_id, player_uuid, player_name, source_server_id, target_server_id, event_type, status, detail, created_at)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, requestId);
			statement.setString(2, playerId.toString());
			statement.setString(3, playerName);
			statement.setString(4, sourceServerId);
			statement.setString(5, targetServerId);
			statement.setString(6, eventType);
			statement.setString(7, status == null ? "UNKNOWN" : status.name());
			statement.setString(8, detail);
			statement.setTimestamp(9, Timestamp.from(createdAt));
			statement.executeUpdate();
		}
	}

	private TransferHistoryEntry mapTransferHistoryEntry(ResultSet resultSet) throws Exception {
		String status = resultSet.getString("status");
		return new TransferHistoryEntry(
				resultSet.getLong("id"),
				resultSet.getString("request_id"),
				UUID.fromString(resultSet.getString("player_uuid")),
				resultSet.getString("player_name"),
				resultSet.getString("source_server_id"),
				resultSet.getString("target_server_id"),
				resultSet.getString("event_type"),
				status == null || status.isBlank() || "UNKNOWN".equalsIgnoreCase(status) ? null : TeleportHandoffStatus.valueOf(status),
				resultSet.getString("detail"),
				resultSet.getTimestamp("created_at").toInstant()
		);
	}

	private Instant toInstant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	private BigDecimal normalizeAmount(double amount) {
		return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal parseBalance(String payload) {
		if (payload == null || payload.isBlank()) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		try {
			return normalizeAmount(Double.parseDouble(payload));
		} catch (NumberFormatException ignored) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
	}

	private String formatBalance(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private String normalizeSource(String source) {
		if (source == null || source.isBlank()) {
			return "api";
		}
		return source.length() > 64 ? source.substring(0, 64) : source;
	}

	private String normalizeServerId(String serverId) {
		if (serverId == null || serverId.isBlank()) {
			return null;
		}
		return serverId.length() > 64 ? serverId.substring(0, 64) : serverId;
	}

	private String normalizeEventType(String eventType) {
		if (eventType == null || eventType.isBlank()) {
			return "unknown";
		}
		return eventType.length() > 32 ? eventType.substring(0, 32) : eventType;
	}

	private String normalizeDetail(String detail) {
		if (detail == null || detail.isBlank()) {
			return null;
		}
		return detail.length() > 255 ? detail.substring(0, 255) : detail;
	}

	private String normalizePlayerName(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return null;
		}
		return playerName;
	}
}
