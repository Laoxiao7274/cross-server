package org.xiaoziyi.crossserver.session;

import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.messaging.MessagingProvider;
import org.xiaoziyi.crossserver.messaging.SyncMessage;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SessionService {
	private final Logger logger;
	private final StorageProvider storageProvider;
	private final MessagingProvider messagingProvider;
	private final String serverId;
	private final PluginConfiguration.SessionSettings settings;
	private final Map<UUID, String> localSessions;
	private final Map<UUID, String> remoteSessionOwners;
	private final Map<UUID, String> preparedTransfers;

	public SessionService(
			Logger logger,
			StorageProvider storageProvider,
			MessagingProvider messagingProvider,
			String serverId,
			PluginConfiguration.SessionSettings settings
	) {
		this.logger = logger;
		this.storageProvider = storageProvider;
		this.messagingProvider = messagingProvider;
		this.serverId = serverId;
		this.settings = settings;
		this.localSessions = new ConcurrentHashMap<>();
		this.remoteSessionOwners = new ConcurrentHashMap<>();
		this.preparedTransfers = new ConcurrentHashMap<>();
	}

	public boolean openPlayerSession(UUID playerId) throws Exception {
		String sessionId = UUID.randomUUID().toString();
		boolean acquired = storageProvider.tryAcquireSession(playerId, serverId, sessionId, Duration.ofSeconds(settings.lockSeconds()));
		if (!acquired) {
			return false;
		}
		localSessions.put(playerId, sessionId);
		preparedTransfers.remove(playerId);
		publishSessionMessage(playerId, "acquired");
		return true;
	}

	public boolean openTransferredSession(UUID playerId, String transferToken) throws Exception {
		String sessionId = UUID.randomUUID().toString();
		boolean acquired = storageProvider.tryClaimTransferredSession(playerId, serverId, sessionId, transferToken, Duration.ofSeconds(settings.lockSeconds()));
		if (!acquired) {
			logger.warning("会话接力 claim 失败: player=" + playerId + " server=" + serverId);
			return false;
		}
		localSessions.put(playerId, sessionId);
		preparedTransfers.remove(playerId);
		logger.info("会话接力 claim 成功: player=" + playerId + " server=" + serverId);
		publishSessionMessage(playerId, "transferred");
		return true;
	}

	public String prepareTransfer(UUID playerId, String targetServerId, Duration ttl) throws Exception {
		String sessionId = localSessions.get(playerId);
		if (sessionId == null) {
			throw new IllegalStateException("玩家当前没有本地会话，无法准备接力");
		}
		String transferToken = storageProvider.prepareSessionTransfer(playerId, sessionId, targetServerId, ttl);
		preparedTransfers.put(playerId, transferToken);
		return transferToken;
	}

	public void clearPreparedTransfer(UUID playerId) {
		String sessionId = localSessions.get(playerId);
		try {
			if (sessionId != null) {
				storageProvider.clearPreparedTransfer(playerId, sessionId);
			} else {
				storageProvider.clearPreparedTransfer(playerId);
			}
		} catch (Exception exception) {
			logger.warning("清理会话接力标记失败: " + playerId + " -> " + exception.getMessage());
		} finally {
			preparedTransfers.remove(playerId);
		}
	}

	public boolean isTransferDeparture(UUID playerId) {
		return preparedTransfers.containsKey(playerId);
	}

	public boolean hasPreparedTransfer(UUID playerId) {
		return preparedTransfers.containsKey(playerId);
	}

	public Optional<SessionTransferState> getSessionTransferState(UUID playerId) throws Exception {
		return storageProvider.loadSessionTransferState(playerId);
	}

	public int getPreparedTransferCount() {
		return preparedTransfers.size();
	}

	public void discardLocalSession(UUID playerId) {
		localSessions.remove(playerId);
		preparedTransfers.remove(playerId);
	}

	public void closePlayerSession(UUID playerId) {
		String sessionId = localSessions.remove(playerId);
		preparedTransfers.remove(playerId);
		if (sessionId == null) {
			return;
		}
		try {
			storageProvider.releaseSession(playerId, sessionId);
			publishSessionMessage(playerId, "released");
		} catch (Exception exception) {
			logger.warning("释放玩家会话失败: " + playerId + " -> " + exception.getMessage());
		}
	}

	public void refreshLocalSessions() {
		if (localSessions.isEmpty()) {
			return;
		}
		Duration ttl = Duration.ofSeconds(settings.lockSeconds());
		for (Map.Entry<UUID, String> entry : localSessions.entrySet()) {
			try {
				storageProvider.refreshSession(entry.getKey(), entry.getValue(), ttl);
			} catch (Exception exception) {
				logger.warning("刷新会话锁失败: " + entry.getKey() + " -> " + exception.getMessage());
			}
		}
	}

	public void handleIncomingMessage(SyncMessage message) {
		if (serverId.equals(message.sourceServerId())) {
			return;
		}
		if (!"session".equalsIgnoreCase(message.namespace()) || !"player".equalsIgnoreCase(message.targetType())) {
			return;
		}
		UUID playerId;
		try {
			playerId = UUID.fromString(message.targetId());
		} catch (IllegalArgumentException exception) {
			logger.warning("收到无效的会话消息玩家 ID: " + message.targetId());
			return;
		}
		if ("acquired".equalsIgnoreCase(message.action()) || "transferred".equalsIgnoreCase(message.action())) {
			remoteSessionOwners.put(playerId, message.sourceServerId() + "|" + message.occurredAt().getEpochSecond());
		} else if ("released".equalsIgnoreCase(message.action())) {
			remoteSessionOwners.remove(playerId, message.sourceServerId() + "|" + message.occurredAt().getEpochSecond());
			remoteSessionOwners.entrySet().removeIf(entry -> entry.getKey().equals(playerId) && entry.getValue().startsWith(message.sourceServerId() + "|"));
		} else {
			return;
		}
		logger.log(Level.FINE, "收到会话消息: player={0} source={1} action={2}", new Object[]{playerId, message.sourceServerId(), message.action()});
	}

	public void pruneRemoteSessions() {
		long threshold = Instant.now().minusSeconds(settings.lockSeconds() * 2L).getEpochSecond();
		remoteSessionOwners.entrySet().removeIf(entry -> {
			String[] parts = entry.getValue().split("\\|", 2);
			if (parts.length < 2) {
				return true;
			}
			try {
				return Long.parseLong(parts[1]) < threshold;
			} catch (NumberFormatException exception) {
				return true;
			}
		});
	}

	public int getRemoteSessionCount() {
		pruneRemoteSessions();
		return remoteSessionOwners.size();
	}

	public int getLocalSessionCount() {
		return localSessions.size();
	}

	public String getServerId() {
		return serverId;
	}

	private void publishSessionMessage(UUID playerId, String action) {
		messagingProvider.publish(new SyncMessage(
				serverId,
				"player",
				playerId.toString(),
				"session",
				action,
				0L,
				Instant.now()
		));
	}
}
