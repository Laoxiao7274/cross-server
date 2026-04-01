package org.xiaoziyi.crossserver.node;

import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.model.NodeStatusRecord;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public final class NodeStatusService {
	private final Logger logger;
	private final StorageProvider storageProvider;
	private final PluginConfiguration.ServerSettings serverSettings;
	private final PluginConfiguration.NodeSettings nodeSettings;

	public NodeStatusService(
			Logger logger,
			StorageProvider storageProvider,
			PluginConfiguration.ServerSettings serverSettings,
			PluginConfiguration.NodeSettings nodeSettings
	) {
		this.logger = logger;
		this.storageProvider = storageProvider;
		this.serverSettings = serverSettings;
		this.nodeSettings = nodeSettings;
	}

	public void heartbeat() {
		Instant now = Instant.now();
		try {
			storageProvider.upsertNodeStatus(serverSettings.id(), serverSettings.cluster(), now, now);
		} catch (Exception exception) {
			logger.warning("更新节点心跳失败: " + serverSettings.id() + " -> " + exception.getMessage());
		}
	}

	public List<NodeStatusRecord> listNodes() throws Exception {
		List<NodeStatusRecord> records = storageProvider.listNodeStatuses(serverSettings.cluster());
		Instant threshold = Instant.now().minusSeconds(nodeSettings.offlineSeconds());
		return records.stream()
				.map(record -> new NodeStatusRecord(
						record.serverId(),
						record.cluster(),
						record.lastSeen().isAfter(threshold) ? "online" : "offline",
						record.latencyMillis(),
						record.lastSeen(),
						record.updatedAt()
				))
				.sorted(
						Comparator.comparing((NodeStatusRecord record) -> !"online".equals(record.status()))
								.thenComparing(NodeStatusRecord::lastSeen, Comparator.reverseOrder())
								.thenComparing(NodeStatusRecord::serverId)
				)
				.toList();
	}

	public NodeStatusRecord getNode(String serverId) throws Exception {
		return listNodes().stream()
				.filter(record -> record.serverId().equalsIgnoreCase(serverId))
				.findFirst()
				.orElse(null);
	}
}
