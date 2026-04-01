package org.xiaoziyi.crossserver.node;

import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.model.NodeStatusRecord;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class NodeIdentityGuardService {
	private final StorageProvider storageProvider;
	private final PluginConfiguration.ServerSettings serverSettings;
	private final PluginConfiguration.NodeSettings nodeSettings;

	public NodeIdentityGuardService(
			StorageProvider storageProvider,
			PluginConfiguration.ServerSettings serverSettings,
			PluginConfiguration.NodeSettings nodeSettings
	) {
		this.storageProvider = storageProvider;
		this.serverSettings = serverSettings;
		this.nodeSettings = nodeSettings;
	}

	public void assertStartupAllowed() throws Exception {
		Optional<NodeStatusRecord> conflict = findActiveConflict();
		if (conflict.isEmpty()) {
			return;
		}
		NodeStatusRecord record = conflict.get();
		throw new IllegalStateException(
				"检测到重复 server.id 节点仍在线: id=" + serverSettings.id()
						+ " cluster=" + serverSettings.cluster()
						+ " lastSeen=" + record.lastSeen()
						+ "，请关闭旧节点或等待 offline-seconds 超时后再启动"
		);
	}

	private Optional<NodeStatusRecord> findActiveConflict() throws Exception {
		List<NodeStatusRecord> records = storageProvider.listNodeStatuses(serverSettings.cluster());
		Instant threshold = Instant.now().minusSeconds(nodeSettings.offlineSeconds());
		String currentId = serverSettings.id().toLowerCase(Locale.ROOT);
		return records.stream()
				.filter(record -> record.serverId() != null && record.serverId().toLowerCase(Locale.ROOT).equals(currentId))
				.filter(record -> record.lastSeen() != null && record.lastSeen().isAfter(threshold))
				.findFirst();
	}
}
