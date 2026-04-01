package org.xiaoziyi.crossserver.sync;

import org.xiaoziyi.crossserver.messaging.MessagingProvider;
import org.xiaoziyi.crossserver.messaging.SyncMessage;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SyncService {
	@FunctionalInterface
	public interface SyncListener {
		void onMessage(SyncMessage message);
	}

	private record SyncListenerRegistration(String namespace, SyncListener listener) {
	}

	private final Logger logger;
	private final StorageProvider storageProvider;
	private final MessagingProvider messagingProvider;
	private final SyncNamespaceRegistry namespaceRegistry;
	private final String serverId;
	private final Set<String> localInvalidations;
	private final Set<String> localInvalidationScopes;
	private final List<SyncListenerRegistration> syncListeners;

	public SyncService(
			Logger logger,
			StorageProvider storageProvider,
			MessagingProvider messagingProvider,
			SyncNamespaceRegistry namespaceRegistry,
			String serverId
	) {
		this.logger = logger;
		this.storageProvider = storageProvider;
		this.messagingProvider = messagingProvider;
		this.namespaceRegistry = namespaceRegistry;
		this.serverId = serverId;
		this.localInvalidations = ConcurrentHashMap.newKeySet();
		this.localInvalidationScopes = ConcurrentHashMap.newKeySet();
		this.syncListeners = new CopyOnWriteArrayList<>();
	}

	public Optional<PlayerSnapshot> loadPlayerData(UUID playerId, String namespace) throws Exception {
		ensureNamespace(namespace);
		Optional<PlayerSnapshot> snapshot = storageProvider.loadPlayerData(playerId, namespace);
		snapshot.ifPresent(value -> consumeLocalInvalidation("player", playerId.toString(), namespace, value.version()));
		return snapshot;
	}

	public PlayerSnapshot savePlayerData(UUID playerId, String namespace, String payload) throws Exception {
		ensureNamespace(namespace);
		PlayerSnapshot snapshot = storageProvider.savePlayerData(playerId, namespace, payload);
		publishPlayerInvalidation(playerId, namespace, snapshot.version());
		return snapshot;
	}

	public Optional<GlobalSnapshot> loadGlobalData(String namespace, String dataKey) throws Exception {
		ensureNamespace(namespace);
		Optional<GlobalSnapshot> snapshot = storageProvider.loadGlobalData(namespace, dataKey);
		snapshot.ifPresent(value -> consumeLocalInvalidation("global", dataKey, namespace, value.version()));
		return snapshot;
	}

	public GlobalSnapshot saveGlobalData(String namespace, String dataKey, String payload) throws Exception {
		ensureNamespace(namespace);
		GlobalSnapshot snapshot = storageProvider.saveGlobalData(namespace, dataKey, payload);
		publishGlobalInvalidation(namespace, dataKey, snapshot.version());
		return snapshot;
	}

	public void publishPlayerInvalidation(UUID playerId, String namespace, long version) {
		markLocalInvalidation("player", playerId.toString(), namespace, version);
		messagingProvider.publish(new SyncMessage(
				serverId,
				"player",
				playerId.toString(),
				namespace,
				"invalidate",
				version,
				Instant.now()
		));
	}

	public void publishGlobalInvalidation(String namespace, String dataKey, long version) {
		markLocalInvalidation("global", dataKey, namespace, version);
		messagingProvider.publish(new SyncMessage(
				serverId,
				"global",
				dataKey,
				namespace,
				"invalidate",
				version,
				Instant.now()
		));
	}

	public void registerListener(String namespace, SyncListener listener) {
		ensureNamespace(namespace);
		syncListeners.add(new SyncListenerRegistration(namespace, listener));
	}

	public void registerListener(SyncListener listener) {
		syncListeners.add(new SyncListenerRegistration(null, listener));
	}

	public Runnable registerListenerHandle(String namespace, SyncListener listener) {
		ensureNamespace(namespace);
		SyncListenerRegistration registration = new SyncListenerRegistration(namespace, listener);
		syncListeners.add(registration);
		return () -> syncListeners.remove(registration);
	}

	public Runnable registerListenerHandle(SyncListener listener) {
		SyncListenerRegistration registration = new SyncListenerRegistration(null, listener);
		syncListeners.add(registration);
		return () -> syncListeners.remove(registration);
	}

	public void handleIncomingMessage(SyncMessage message) {
		if (serverId.equals(message.sourceServerId())) {
			return;
		}
		if (!namespaceRegistry.isRegistered(message.namespace())) {
			return;
		}
		if ("invalidate".equalsIgnoreCase(message.action())) {
			markLocalInvalidation(message.targetType(), message.targetId(), message.namespace(), message.version());
		}
		notifyListeners(message);
		logger.log(Level.FINE, "收到同步消息: {0}:{1} namespace={2} version={3} action={4}", new Object[]{message.targetType(), message.targetId(), message.namespace(), message.version(), message.action()});
	}

	public boolean consumeLocalInvalidation(String targetType, String targetId, String namespace, long version) {
		boolean removed = localInvalidations.remove(invalidationKey(targetType, targetId, namespace, version));
		if (removed) {
			clearInvalidationScopeIfUnused(scopeKey(targetType, targetId, namespace));
		}
		return removed;
	}

	public int getPendingInvalidationCount() {
		return localInvalidationScopes.size();
	}

	private void markLocalInvalidation(String targetType, String targetId, String namespace, long version) {
		String scopeKey = scopeKey(targetType, targetId, namespace);
		removeInvalidations(scope -> scope.equals(scopeKey));
		localInvalidations.add(invalidationKey(targetType, targetId, namespace, version));
		localInvalidationScopes.add(scopeKey);
	}

	private String invalidationKey(String targetType, String targetId, String namespace, long version) {
		return scopeKey(targetType, targetId, namespace) + "|" + version;
	}

	private String scopeKey(String targetType, String targetId, String namespace) {
		return targetType + "|" + targetId + "|" + namespace;
	}

	private void removeInvalidations(Predicate<String> predicate) {
		localInvalidations.removeIf(predicate);
	}

	private void clearInvalidationScopeIfUnused(String scopeKey) {
		boolean stillPresent = localInvalidations.stream().anyMatch(key -> key.startsWith(scopeKey + "|"));
		if (!stillPresent) {
			localInvalidationScopes.remove(scopeKey);
		}
	}

	private void notifyListeners(SyncMessage message) {
		for (SyncListenerRegistration registration : syncListeners) {
			if (registration.namespace() != null && !registration.namespace().equalsIgnoreCase(message.namespace())) {
				continue;
			}
			try {
				registration.listener().onMessage(message);
			} catch (Exception exception) {
				logger.warning("处理同步监听器失败: " + exception.getMessage());
			}
		}
	}

	private void ensureNamespace(String namespace) {
		if (!namespaceRegistry.isRegistered(namespace)) {
			throw new IllegalArgumentException("未注册的命名空间: " + namespace);
		}
	}
}
