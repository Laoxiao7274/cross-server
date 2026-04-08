package org.xiaoziyi.crossserver.configcenter;

import org.bukkit.Bukkit;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.bootstrap.CrossServerPlugin;
import org.xiaoziyi.crossserver.config.NodeLocalConfigService;
import org.xiaoziyi.crossserver.config.PluginConfiguration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NodeConfigSyncService implements AutoCloseable {
	public static final String NAMESPACE = "node.config";
	private static final String SNAPSHOT_PREFIX = "snapshot.";
	private static final String APPLY_PREFIX = "apply.";
	private static final int SCHEMA_VERSION = 1;
	private static final String SOURCE = "crossserver.node-config";

	private final CrossServerPlugin plugin;
	private final CrossServerApi api;
	private final PluginConfiguration.ServerSettings serverSettings;
	private final NodeLocalConfigService localConfigService;
	private Runnable applyListenerCleanup;

	public NodeConfigSyncService(
			CrossServerPlugin plugin,
			CrossServerApi api,
			PluginConfiguration.ServerSettings serverSettings,
			NodeLocalConfigService localConfigService
	) {
		this.plugin = plugin;
		this.api = api;
		this.serverSettings = serverSettings;
		this.localConfigService = localConfigService;
		this.api.registerConfigDocument(NAMESPACE, snapshotKey(serverSettings.id()));
		this.api.registerConfigDocument(NAMESPACE, applyKey(serverSettings.id()));
		this.applyListenerCleanup = this.api.registerConfigDocumentListener(NAMESPACE, applyKey(serverSettings.id()), event -> handleApplySignal());
	}

	public void publishLocalSnapshot(PluginConfiguration configuration) throws Exception {
		NodeConfigSnapshot snapshot = new NodeConfigSnapshot(
				SCHEMA_VERSION,
				serverSettings.id(),
				Map.copyOf(localConfigService.exportEditableConfig(configuration)),
				Instant.now(),
				serverSettings.id(),
				SOURCE,
				"同步节点本地配置快照"
		);
		api.saveConfigDocument(
				NAMESPACE,
				snapshotKey(serverSettings.id()),
				new ConfigDocumentUpdate(
						NodeConfigDocumentCodec.encodeSnapshot(snapshot),
						SCHEMA_VERSION,
						snapshot.updatedBy(),
						SOURCE,
						snapshot.summary()
				)
		);
	}

	public Map<String, Object> loadClusterNodeConfigs() {
		List<Map<String, Object>> nodes = new ArrayList<>();
		for (RegisteredConfigDocument document : api.getRegisteredConfigDocuments()) {
			if (!NAMESPACE.equals(document.namespace()) || !document.dataKey().startsWith(SNAPSHOT_PREFIX)) {
				continue;
			}
			try {
				Optional<ConfigDocument> loaded = api.loadConfigDocument(document.namespace(), document.dataKey());
				if (loaded.isEmpty()) {
					continue;
				}
				NodeConfigSnapshot snapshot = NodeConfigDocumentCodec.decodeSnapshot(loaded.get().payload());
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("serverId", snapshot.serverId());
				item.put("capturedAt", snapshot.capturedAt());
				item.put("editableConfig", snapshot.editableConfig());
				item.put("local", snapshot.serverId() != null && snapshot.serverId().equalsIgnoreCase(serverSettings.id()));
				loadLatestApply(snapshot.serverId()).ifPresent(request -> item.put("latestApply", requestToMap(request)));
				nodes.add(item);
			} catch (Exception ignored) {
			}
		}
		nodes.sort((left, right) -> String.valueOf(left.get("serverId")).compareToIgnoreCase(String.valueOf(right.get("serverId"))));
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("localServerId", serverSettings.id());
		result.put("nodes", nodes);
		result.put("updatedAt", Instant.now());
		return result;
	}

	public Map<String, Object> loadNodeConfigDetail(String serverId) {
		String normalizedServerId = requireServerId(serverId);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("serverId", normalizedServerId);
		loadSnapshot(normalizedServerId).ifPresent(snapshot -> result.put("snapshot", snapshotToMap(snapshot)));
		loadLatestApply(normalizedServerId).ifPresent(request -> result.put("latestApply", requestToMap(request)));
		return result;
	}

	public Map<String, Object> requestApply(String targetServerId, Map<String, Object> requestChanges, String actorName) throws Exception {
		String normalizedServerId = requireServerId(targetServerId);
		Map<String, Object> normalizedChanges = localConfigService.normalizeChanges(requestChanges);
		String actor = actorName == null || actorName.isBlank() ? "web-panel" : actorName.trim();
		NodeConfigApplyRequest request = new NodeConfigApplyRequest(
				SCHEMA_VERSION,
				UUID.randomUUID().toString(),
				normalizedServerId,
				Map.copyOf(normalizedChanges),
				"pending",
				actor,
				Instant.now(),
				null,
				null,
				"等待目标节点应用配置",
				SOURCE,
				"提交节点配置应用请求"
		);
		api.saveConfigDocument(
				NAMESPACE,
				applyKey(normalizedServerId),
				new ConfigDocumentUpdate(
						NodeConfigDocumentCodec.encodeApplyRequest(request),
						SCHEMA_VERSION,
						actor,
						SOURCE,
						request.summary()
				)
		);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("accepted", true);
		result.put("requestId", request.requestId());
		result.put("targetServerId", normalizedServerId);
		result.put("status", request.status());
		return result;
	}

	private void handleApplySignal() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				applyPendingRequest();
			} catch (Exception exception) {
				plugin.getLogger().warning("处理节点配置申请失败: " + exception.getMessage());
			}
		});
	}

	private void applyPendingRequest() throws Exception {
		Optional<NodeConfigApplyRequest> loaded = loadLatestApply(serverSettings.id());
		if (loaded.isEmpty()) {
			return;
		}
		NodeConfigApplyRequest request = loaded.get();
		if (!"pending".equalsIgnoreCase(request.status())) {
			return;
		}
		NodeConfigApplyRequest applying = withStatus(request, "applying", "目标节点正在写入 config.yml");
		saveApplyRequest(applying);
		try {
			localConfigService.applyChanges(request.changes());
			NodeConfigApplyRequest success = new NodeConfigApplyRequest(
					SCHEMA_VERSION,
					request.requestId(),
					request.targetServerId(),
					request.changes(),
					"applied",
					request.requestedBy(),
					request.requestedAt(),
					Instant.now(),
					serverSettings.id(),
					plugin.requestReload(request.requestedBy(), "node-config-apply") ? "节点已写入配置，并已加入重载队列" : "节点已写入配置，但当前已有重载进行中",
					SOURCE,
					"节点配置已应用"
			);
			saveApplyRequest(success);
		} catch (Exception exception) {
			NodeConfigApplyRequest failed = new NodeConfigApplyRequest(
					SCHEMA_VERSION,
					request.requestId(),
					request.targetServerId(),
					request.changes(),
					"failed",
					request.requestedBy(),
					request.requestedAt(),
					Instant.now(),
					serverSettings.id(),
					exception.getMessage() == null ? "节点配置应用失败" : exception.getMessage(),
					SOURCE,
					"节点配置应用失败"
			);
			saveApplyRequest(failed);
		}
	}

	private void saveApplyRequest(NodeConfigApplyRequest request) throws Exception {
		api.saveConfigDocument(
				NAMESPACE,
				applyKey(request.targetServerId()),
				new ConfigDocumentUpdate(
						NodeConfigDocumentCodec.encodeApplyRequest(request),
						SCHEMA_VERSION,
						request.appliedBy() != null ? request.appliedBy() : request.requestedBy(),
						SOURCE,
						request.summary()
				)
		);
	}

	private NodeConfigApplyRequest withStatus(NodeConfigApplyRequest request, String status, String message) {
		return new NodeConfigApplyRequest(
				SCHEMA_VERSION,
				request.requestId(),
				request.targetServerId(),
				request.changes(),
				status,
				request.requestedBy(),
				request.requestedAt(),
				null,
				serverSettings.id(),
				message,
				SOURCE,
				message
		);
	}

	private Optional<NodeConfigSnapshot> loadSnapshot(String serverId) {
		try {
			Optional<ConfigDocument> document = api.loadConfigDocument(NAMESPACE, snapshotKey(serverId));
			if (document.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(NodeConfigDocumentCodec.decodeSnapshot(document.get().payload()));
		} catch (Exception exception) {
			return Optional.empty();
		}
	}

	private Optional<NodeConfigApplyRequest> loadLatestApply(String serverId) {
		try {
			Optional<ConfigDocument> document = api.loadConfigDocument(NAMESPACE, applyKey(serverId));
			if (document.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(NodeConfigDocumentCodec.decodeApplyRequest(document.get().payload()));
		} catch (Exception exception) {
			return Optional.empty();
		}
	}

	private Map<String, Object> snapshotToMap(NodeConfigSnapshot snapshot) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("serverId", snapshot.serverId());
		result.put("capturedAt", snapshot.capturedAt());
		result.put("editableConfig", snapshot.editableConfig());
		result.put("updatedBy", snapshot.updatedBy());
		result.put("summary", snapshot.summary());
		return result;
	}

	private Map<String, Object> requestToMap(NodeConfigApplyRequest request) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("requestId", request.requestId());
		result.put("targetServerId", request.targetServerId());
		result.put("changes", request.changes());
		result.put("status", request.status());
		result.put("requestedBy", request.requestedBy());
		result.put("requestedAt", request.requestedAt());
		result.put("appliedAt", request.appliedAt());
		result.put("appliedBy", request.appliedBy());
		result.put("resultMessage", request.resultMessage());
		return result;
	}

	private String snapshotKey(String serverId) {
		return SNAPSHOT_PREFIX + requireServerId(serverId);
	}

	private String applyKey(String serverId) {
		return APPLY_PREFIX + requireServerId(serverId);
	}

	private String requireServerId(String serverId) {
		if (serverId == null || serverId.isBlank()) {
			throw new IllegalArgumentException("serverId 不能为空");
		}
		return serverId.trim();
	}

	@Override
	public void close() {
		if (applyListenerCleanup != null) {
			applyListenerCleanup.run();
			applyListenerCleanup = null;
		}
	}
}
