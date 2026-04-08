package org.xiaoziyi.crossserver.web;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.bootstrap.CrossServerPlugin;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.config.RouteTableService;
import org.xiaoziyi.crossserver.config.SharedModuleConfigService;
import org.xiaoziyi.crossserver.config.SharedModuleConfigSnapshot;
import org.xiaoziyi.crossserver.configcenter.ConfigDocument;
import org.xiaoziyi.crossserver.configcenter.NodeConfigSyncService;
import org.xiaoziyi.crossserver.configcenter.RegisteredConfigDocument;
import org.xiaoziyi.crossserver.model.NodeStatusRecord;
import org.xiaoziyi.crossserver.node.NodeStatusService;
import org.xiaoziyi.crossserver.session.SessionService;
import org.xiaoziyi.crossserver.storage.StorageProvider;
import org.xiaoziyi.crossserver.sync.SyncNamespaceRegistry;
import org.xiaoziyi.crossserver.sync.SyncService;
import org.xiaoziyi.crossserver.teleport.TransferAdminService;
import org.xiaoziyi.crossserver.teleport.TransferHistoryEntry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WebPanelDataService {
	private static final ObjectMapper DOCUMENT_OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	private final CrossServerPlugin plugin;
	private final PluginConfiguration configuration;
	private final SyncNamespaceRegistry namespaceRegistry;
	private final SessionService sessionService;
	private final SyncService syncService;
	private final NodeStatusService nodeStatusService;
	private final RouteTableService routeTableService;
	private final SharedModuleConfigService sharedModuleConfigService;
	private final TransferAdminService transferAdminService;
	private final StorageProvider storageProvider;
	private final CrossServerApi api;
	private final WebPanelClusterService clusterService;
	private final WebPanelLogService logService;
	private final NodeConfigSyncService nodeConfigSyncService;

	public WebPanelDataService(
			CrossServerPlugin plugin,
			PluginConfiguration configuration,
			SyncNamespaceRegistry namespaceRegistry,
			SessionService sessionService,
			SyncService syncService,
			NodeStatusService nodeStatusService,
			RouteTableService routeTableService,
			SharedModuleConfigService sharedModuleConfigService,
			TransferAdminService transferAdminService,
			StorageProvider storageProvider,
			CrossServerApi api,
			WebPanelClusterService clusterService,
			WebPanelLogService logService,
			NodeConfigSyncService nodeConfigSyncService
	) {
		this.plugin = plugin;
		this.configuration = configuration;
		this.namespaceRegistry = namespaceRegistry;
		this.sessionService = sessionService;
		this.syncService = syncService;
		this.nodeStatusService = nodeStatusService;
		this.routeTableService = routeTableService;
		this.sharedModuleConfigService = sharedModuleConfigService;
		this.transferAdminService = transferAdminService;
		this.storageProvider = storageProvider;
		this.api = api;
		this.clusterService = clusterService;
		this.logService = logService;
		this.nodeConfigSyncService = nodeConfigSyncService;
	}

	public Map<String, Object> loadOverview() throws Exception {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", loadStatus());
		result.put("modules", loadModules());
		result.put("routes", loadRoutes());
		result.put("documents", loadConfigDocuments());
		result.put("recentTransfers", loadRecentTransfers());
		result.put("logs", loadLogs());
		result.put("nodeConfigs", loadNodeConfigs());
		return result;
	}

	public Map<String, Object> loadStatus() throws Exception {
		List<Map<String, Object>> nodes = new ArrayList<>();
		for (NodeStatusRecord record : nodeStatusService.listNodes()) {
			nodes.add(nodeToMap(record));
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("serverId", configuration.server().id());
		result.put("cluster", configuration.server().cluster());
		result.put("namespaceCount", namespaceRegistry.getNamespaces().size());
		result.put("localSessionCount", sessionService.getLocalSessionCount());
		result.put("remoteSessionCount", sessionService.getRemoteSessionCount());
		result.put("preparedTransferCount", sessionService.getPreparedTransferCount());
		result.put("teleportGatewayType", configuration.teleport().gateway().type());
		result.put("handoffSeconds", configuration.teleport().handoffSeconds());
		result.put("redisEnabled", configuration.messaging().enabled());
		result.put("pendingInvalidationCount", syncService.getPendingInvalidationCount());
		result.put("nodes", nodes);
		result.put("webPanelCluster", loadWebPanelCluster());
		return result;
	}

	public Map<String, Object> loadModules() {
		Map<String, Object> result = new LinkedHashMap<>();
		PluginConfiguration.ModuleSettings local = configuration.modules();
		Optional<SharedModuleConfigSnapshot> shared = sharedModuleConfigService.loadSharedConfig();
		result.put("local", modulesToMap(local));
		result.put("shared", shared.map(this::sharedModulesToMap).orElse(Map.of()));
		result.put("effective", modulesToMap(sharedModuleConfigService.mergeInto(configuration).modules()));
		return result;
	}

	public Map<String, Object> loadRoutes() {
		Map<String, String> localRoutes = configuration.teleport().gateway().serverMap();
		Map<String, String> sharedRoutes = routeTableService.loadSharedRoutes();
		Map<String, String> effectiveRoutes = routeTableService.mergedRoutes(configuration);
		List<Map<String, Object>> entries = new ArrayList<>();
		Set<String> serverIds = new java.util.TreeSet<>();
		serverIds.addAll(localRoutes.keySet());
		serverIds.addAll(sharedRoutes.keySet());
		serverIds.addAll(effectiveRoutes.keySet());
		for (String serverId : serverIds) {
			boolean local = localRoutes.containsKey(serverId);
			boolean shared = sharedRoutes.containsKey(serverId);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("serverId", serverId);
			entry.put("localTarget", localRoutes.get(serverId));
			entry.put("sharedTarget", sharedRoutes.get(serverId));
			entry.put("effectiveTarget", effectiveRoutes.get(serverId));
			entry.put("source", routeSource(local, shared));
			entries.add(entry);
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("local", localRoutes);
		result.put("shared", sharedRoutes);
		result.put("effective", effectiveRoutes);
		result.put("entries", entries);
		return result;
	}

	public Map<String, Object> updateModules(Map<String, Object> request, String actorName) throws Exception {
		Objects.requireNonNull(request, "request");
		SharedModuleConfigSnapshot current = sharedModuleConfigService.loadSharedConfig().orElse(null);
		SharedModuleConfigSnapshot updated = new SharedModuleConfigSnapshot(
				1,
				readBoolean(request, "auth", current != null ? current.auth() : null),
				readBoolean(request, "homes", current != null ? current.homes() : null),
				readBoolean(request, "warps", current != null ? current.warps() : null),
				readBoolean(request, "tpa", current != null ? current.tpa() : null),
				readBoolean(request, "routeConfig", current != null ? current.routeConfig() : null),
				readBoolean(request, "transferAdmin", current != null ? current.transferAdmin() : null),
				readBoolean(request, "economyBridge", current != null ? current.economyBridge() : null),
				readBoolean(request, "permissions", current != null ? current.permissions() : null),
				actorName,
				null,
				null,
				null
		);
		sharedModuleConfigService.saveSharedConfig(updated, actorName);
		return loadModules();
	}

	public Map<String, String> replaceRoutes(Map<String, Object> request, String actorName) throws Exception {
		Objects.requireNonNull(request, "request");
		Object routesValue = request.get("routes");
		if (!(routesValue instanceof Map<?, ?> rawRoutes)) {
			throw new IllegalArgumentException("routes 字段必须是对象");
		}
		Map<String, String> routes = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : rawRoutes.entrySet()) {
			String serverId = normalizeText(entry.getKey(), "serverId");
			String proxyTarget = normalizeText(entry.getValue(), "proxyTarget");
			routes.put(serverId, proxyTarget);
		}
		routeTableService.saveSharedRoutes(routes, actorName);
		return routeTableService.loadSharedRoutes();
	}

	public Map<String, String> upsertRoute(Map<String, Object> request, String actorName) throws Exception {
		Objects.requireNonNull(request, "request");
		String serverId = normalizeText(request.get("serverId"), "serverId");
		String proxyTarget = normalizeText(request.get("proxyTarget"), "proxyTarget");
		routeTableService.setSharedRoute(serverId, proxyTarget, actorName);
		return routeTableService.loadSharedRoutes();
	}

	public Map<String, Object> deleteRoute(String serverId, String actorName) throws Exception {
		boolean removed = routeTableService.removeSharedRoute(serverId, actorName);
		return Map.of(
				"removed", removed,
				"routes", routeTableService.loadSharedRoutes()
		);
	}

	public Map<String, Object> requestReload(Map<String, Object> request, String actorName) {
		Objects.requireNonNull(request, "request");
		Object confirm = request.get("confirm");
		if (!(confirm instanceof String text) || !"reload".equalsIgnoreCase(text.trim())) {
			throw new IllegalArgumentException("confirm 必须为 'reload'");
		}
		boolean accepted = plugin.requestReload(actorName, "web-panel");
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("accepted", accepted);
		result.put("status", accepted ? "queued" : "already_reloading");
		result.put("message", accepted ? "已加入重载队列" : "当前已存在进行中的重载请求");
		result.put("serverId", configuration.server().id());
		result.put("actor", actorName == null || actorName.isBlank() ? "web-panel" : actorName);
		return result;
	}

	public List<Map<String, Object>> loadConfigDocuments() throws Exception {
		Set<RegisteredConfigDocument> registered = api.getRegisteredConfigDocuments();
		List<Map<String, Object>> result = new ArrayList<>();
		for (RegisteredConfigDocument document : registered) {
			Optional<ConfigDocument> loaded = api.loadConfigDocument(document.namespace(), document.dataKey());
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("namespace", document.namespace());
			item.put("dataKey", document.dataKey());
			item.put("title", documentTitle(document.namespace(), document.dataKey()));
			item.put("purpose", documentPurpose(document.namespace(), document.dataKey()));
			item.put("present", loaded.isPresent());
			loaded.ifPresent(value -> {
				item.put("version", value.version());
				item.put("schemaVersion", value.schemaVersion());
				item.put("updatedBy", value.updatedBy());
				item.put("updatedAt", value.updatedAt());
				item.put("source", value.source());
				item.put("summary", value.summary());
				item.put("payload", value.payload());
				item.put("payloadPretty", prettyPayload(value.payload()));
				item.put("payloadType", payloadType(value.payload()));
			});
			result.add(item);
		}
		return result;
	}

	public List<TransferHistoryEntry> loadRecentTransfers() throws Exception {
		if (transferAdminService == null) {
			return List.of();
		}
		return transferAdminService.getRecentTransferHistory(20);
	}

	public Map<String, Object> loadLogs() {
		Map<String, Object> result = new LinkedHashMap<>();
		List<Map<String, Object>> servers = new ArrayList<>();
		Optional<WebPanelClusterSnapshot> snapshot = Optional.empty();
		try {
			snapshot = clusterService.loadSnapshot();
		} catch (Exception ignored) {
		}
		java.util.Set<String> serverIds = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		serverIds.add(configuration.server().id());
		snapshot.ifPresent(value -> serverIds.addAll(value.members().keySet()));
		for (String serverId : serverIds) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("serverId", serverId);
			item.put("local", serverId.equalsIgnoreCase(configuration.server().id()));
			Optional<WebPanelLogSnapshot> logSnapshot = logService == null ? Optional.empty() : logService.loadSnapshot(serverId);
			if (logSnapshot.isPresent()) {
				item.put("updatedAt", logSnapshot.get().updatedAt());
				item.put("entries", logSnapshot.get().entries());
				item.put("entryCount", logSnapshot.get().entries() == null ? 0 : logSnapshot.get().entries().size());
			} else {
				item.put("updatedAt", null);
				item.put("entries", List.of());
				item.put("entryCount", 0);
			}
			servers.add(item);
		}
		result.put("localServerId", configuration.server().id());
		result.put("servers", servers);
		result.put("updatedAt", java.time.Instant.now());
		return result;
	}

	public Map<String, Object> loadNodeConfigs() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("localServerId", configuration.server().id());
		result.put("updatedAt", java.time.Instant.now());
		if (nodeConfigSyncService == null) {
			result.put("nodes", List.of());
			return result;
		}
		Map<String, NodeStatusRecord> nodeStatuses = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		try {
			for (NodeStatusRecord record : nodeStatusService.listNodes()) {
				nodeStatuses.put(record.serverId(), record);
			}
		} catch (Exception ignored) {
		}
		Optional<WebPanelClusterSnapshot> clusterSnapshot = Optional.empty();
		try {
			clusterSnapshot = clusterService.loadSnapshot();
		} catch (Exception ignored) {
		}
		Set<String> serverIds = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		serverIds.add(configuration.server().id());
		serverIds.addAll(nodeStatuses.keySet());
		clusterSnapshot.ifPresent(value -> serverIds.addAll(value.members().keySet()));
		List<Map<String, Object>> nodes = new ArrayList<>();
		for (String serverId : serverIds) {
			Map<String, Object> item = new LinkedHashMap<>(loadNodeConfigDetail(serverId));
			NodeStatusRecord record = nodeStatuses.get(serverId);
			item.put("serverId", serverId);
			item.put("local", serverId.equalsIgnoreCase(configuration.server().id()));
			item.put("hasSnapshot", item.containsKey("snapshot"));
			item.put("nodeStatus", record == null ? null : nodeToMap(record));
			nodes.add(item);
		}
		result.put("nodes", nodes);
		return result;
	}

	public Map<String, Object> loadNodeConfigDetail(String serverId) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("serverId", serverId);
		result.put("local", serverId != null && serverId.equalsIgnoreCase(configuration.server().id()));
		if (nodeConfigSyncService == null) {
			return result;
		}
		result.putAll(nodeConfigSyncService.loadNodeConfigDetail(serverId));
		try {
			NodeStatusRecord record = nodeStatusService.getNode(serverId);
			result.put("nodeStatus", record == null ? null : nodeToMap(record));
		} catch (Exception ignored) {
		}
		return result;
	}

	public Map<String, Object> requestNodeConfigApply(Map<String, Object> request, String actorName) throws Exception {
		if (nodeConfigSyncService == null) {
			throw new IllegalStateException("节点配置管理服务未启用");
		}
		Objects.requireNonNull(request, "request");
		String serverId = normalizeText(request.get("serverId"), "serverId");
		Object changes = request.get("changes");
		if (!(changes instanceof Map<?, ?> rawChanges)) {
			throw new IllegalArgumentException("changes 字段必须是对象");
		}
		Map<String, Object> normalizedChanges = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : rawChanges.entrySet()) {
			if (!(entry.getKey() instanceof String key)) {
				throw new IllegalArgumentException("changes 的键必须是字符串");
			}
			normalizedChanges.put(key, entry.getValue());
		}
		return nodeConfigSyncService.requestApply(serverId, normalizedChanges, actorName);
	}

	public Map<String, Object> loadPlayerTransfer(String playerName) throws Exception {
		if (transferAdminService == null) {
			return Map.of("enabled", false);
		}
		Optional<TransferAdminService.TransferInspection> inspection = transferAdminService.inspectPlayer(playerName);
		if (inspection.isEmpty()) {
			return Map.of("found", false, "playerName", playerName);
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("found", true);
		result.put("inspection", inspection.get());
		return result;
	}

	private Map<String, Object> loadWebPanelCluster() throws Exception {
		Optional<WebPanelClusterSnapshot> snapshot = clusterService.loadSnapshot();
		String masterServerId = configuration.webPanel().masterServerId();
		if (snapshot.isEmpty()) {
			return Map.of(
					"enabled", configuration.webPanel().enabled(),
					"leaderServerId", masterServerId,
					"localServerId", configuration.server().id(),
					"localIsMaster", configuration.webPanel().isMasterServer(configuration.server().id()),
					"heartbeatSeconds", configuration.webPanel().clusterHeartbeatSeconds(),
					"leaseSeconds", configuration.webPanel().clusterLeaseSeconds(),
					"members", List.of()
			);
		}
		List<Map<String, Object>> members = new ArrayList<>();
		for (WebPanelMemberSnapshot member : snapshot.get().members().values()) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("serverId", member.serverId());
			item.put("host", member.host());
			item.put("port", member.port());
			item.put("advertisedAt", member.advertisedAt());
			item.put("leader", member.master());
			members.add(item);
		}
		return Map.of(
				"enabled", configuration.webPanel().enabled(),
				"leaderServerId", snapshot.get().leaderServerId(),
				"localServerId", configuration.server().id(),
				"localIsMaster", configuration.webPanel().isMasterServer(configuration.server().id()),
				"heartbeatSeconds", configuration.webPanel().clusterHeartbeatSeconds(),
				"leaseSeconds", configuration.webPanel().clusterLeaseSeconds(),
				"members", members,
				"updatedAt", snapshot.get().updatedAt()
		);
	}

	private Boolean readBoolean(Map<String, Object> request, String field, Boolean fallback) {
		if (!request.containsKey(field)) {
			return fallback;
		}
		Object value = request.get(field);
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean bool) {
			return bool;
		}
		throw new IllegalArgumentException(field + " 必须是布尔值或 null");
	}

	private String normalizeText(Object value, String field) {
		if (!(value instanceof String text)) {
			throw new IllegalArgumentException(field + " 必须是字符串");
		}
		String normalized = text.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " 不能为空");
		}
		return normalized;
	}

	private String documentTitle(String namespace, String dataKey) {
		if (SharedModuleConfigService.NAMESPACE.equals(namespace) && SharedModuleConfigService.DATA_KEY.equals(dataKey)) {
			return "共享模块开关";
		}
		if (RouteTableService.NAMESPACE.equals(namespace) && RouteTableService.DATA_KEY.equals(dataKey)) {
			return "共享路由表";
		}
		if (WebPanelClusterService.NAMESPACE.equals(namespace) && WebPanelClusterService.DATA_KEY.equals(dataKey)) {
			return "Web 面板集群状态";
		}
		return namespace + " / " + dataKey;
	}

	private String documentPurpose(String namespace, String dataKey) {
		if (SharedModuleConfigService.NAMESPACE.equals(namespace) && SharedModuleConfigService.DATA_KEY.equals(dataKey)) {
			return "控制 auth、homes、warps、tpa 等模块在整个集群内的共享覆盖开关。设为 null 时表示跟随各节点本地配置。";
		}
		if (RouteTableService.NAMESPACE.equals(namespace) && RouteTableService.DATA_KEY.equals(dataKey)) {
			return "维护 serverId 到 proxyTarget 的共享映射，用于跨服传送和路由覆盖。";
		}
		if (WebPanelClusterService.NAMESPACE.equals(namespace) && WebPanelClusterService.DATA_KEY.equals(dataKey)) {
			return "记录哪些节点正在提供 Web 面板，以及当前哪个节点是主面板节点。";
		}
		return "这是已注册到配置中心的文档，当前没有内置的人类可读说明。";
	}

	private String payloadType(String payload) {
		if (payload == null || payload.isBlank()) {
			return "empty";
		}
		try {
			Object value = DOCUMENT_OBJECT_MAPPER.readValue(payload, Object.class);
			if (value instanceof Map<?, ?>) {
				return "json-object";
			}
			if (value instanceof List<?>) {
				return "json-array";
			}
			return "json-value";
		} catch (Exception ignored) {
			return "text";
		}
	}

	private String prettyPayload(String payload) {
		if (payload == null || payload.isBlank()) {
			return "";
		}
		try {
			Object value = DOCUMENT_OBJECT_MAPPER.readValue(payload, Object.class);
			return DOCUMENT_OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
		} catch (Exception ignored) {
			return payload;
		}
	}

	private String routeSource(boolean local, boolean shared) {
		if (local && shared) {
			return "本地 + 共享覆盖";
		}
		if (shared) {
			return "仅共享";
		}
		return "仅本地";
	}

	private Map<String, Object> nodeToMap(NodeStatusRecord record) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("serverId", record.serverId());
		item.put("cluster", record.cluster());
		item.put("status", record.status());
		item.put("latencyMillis", record.latencyMillis());
		item.put("lastSeen", record.lastSeen());
		item.put("updatedAt", record.updatedAt());
		return item;
	}

	private Map<String, Object> modulesToMap(PluginConfiguration.ModuleSettings modules) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("auth", modules.auth());
		item.put("homes", modules.homes());
		item.put("warps", modules.warps());
		item.put("tpa", modules.tpa());
		item.put("routeConfig", modules.routeConfig());
		item.put("transferAdmin", modules.transferAdmin());
		item.put("economyBridge", modules.economyBridge());
		item.put("permissions", modules.permissions());
		return item;
	}

	private Map<String, Object> sharedModulesToMap(SharedModuleConfigSnapshot modules) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("auth", modules.auth());
		item.put("homes", modules.homes());
		item.put("warps", modules.warps());
		item.put("tpa", modules.tpa());
		item.put("routeConfig", modules.routeConfig());
		item.put("transferAdmin", modules.transferAdmin());
		item.put("economyBridge", modules.economyBridge());
		item.put("permissions", modules.permissions());
		item.put("updatedBy", modules.updatedBy());
		item.put("updatedAt", modules.updatedAt());
		item.put("source", modules.source());
		item.put("summary", modules.summary());
		return item;
	}
}
