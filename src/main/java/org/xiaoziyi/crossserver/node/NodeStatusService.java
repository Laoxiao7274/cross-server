package org.xiaoziyi.crossserver.node;

import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.model.NodeStatusRecord;
import org.xiaoziyi.crossserver.storage.StorageProvider;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class NodeStatusService {
	private final Logger logger;
	private final StorageProvider storageProvider;
	private final PluginConfiguration.ServerSettings serverSettings;
	private final PluginConfiguration.NodeSettings nodeSettings;
	private final AtomicBoolean shuttingDown;

	public NodeStatusService(
			Logger logger,
			StorageProvider storageProvider,
			PluginConfiguration.ServerSettings serverSettings,
			PluginConfiguration.NodeSettings nodeSettings,
			AtomicBoolean shuttingDown
	) {
		this.logger = logger;
		this.storageProvider = storageProvider;
		this.serverSettings = serverSettings;
		this.nodeSettings = nodeSettings;
		this.shuttingDown = shuttingDown;
	}

	public void heartbeat() {
		if (shuttingDown.get()) {
			return;
		}
		Instant now = Instant.now();
		try {
			storageProvider.upsertNodeStatus(serverSettings.id(), serverSettings.cluster(), now, now);
		} catch (Exception exception) {
			logger.warning("更新节点心跳失败: " + serverSettings.id() + " -> " + exception.getMessage());
		}
	}

	public void clearLocalNodeStatus() {
		try {
			storageProvider.deleteNodeStatus(serverSettings.id(), serverSettings.cluster());
			Instant threshold = Instant.now().minusSeconds(nodeSettings.offlineSeconds());
			boolean stillPresent = storageProvider.listNodeStatuses(serverSettings.cluster()).stream()
					.anyMatch(record -> serverSettings.id().equalsIgnoreCase(record.serverId())
							&& record.lastSeen() != null && record.lastSeen().isAfter(threshold));
			if (stillPresent) {
				logger.warning("节点状态清理后仍检测到残留记录，尝试二次清理: " + serverSettings.id());
				storageProvider.deleteNodeStatus(serverSettings.id(), serverSettings.cluster());
			}
		} catch (Exception exception) {
			logger.warning("清理节点状态失败: " + serverSettings.id() + " -> " + exception.getMessage());
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
