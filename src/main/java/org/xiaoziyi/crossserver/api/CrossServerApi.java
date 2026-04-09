package org.xiaoziyi.crossserver.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.xiaoziyi.crossserver.auth.AuthService;
import org.xiaoziyi.crossserver.auth.AuthService.AuthAdminInspection;
import org.xiaoziyi.crossserver.auth.AuthTicket;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.config.RouteTableService;
import org.xiaoziyi.crossserver.config.SharedModuleConfigService;
import org.xiaoziyi.crossserver.config.SharedModuleConfigSnapshot;
import org.xiaoziyi.crossserver.configcenter.ConfigCenterService;
import org.xiaoziyi.crossserver.configcenter.ConfigChangeEvent;
import org.xiaoziyi.crossserver.configcenter.ConfigDocument;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentSchema;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentSchemaValidator;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentUpdate;
import org.xiaoziyi.crossserver.configcenter.ConfigEntry;
import org.xiaoziyi.crossserver.configcenter.NodeConfigSyncService;
import org.xiaoziyi.crossserver.homes.HomeEntry;
import org.xiaoziyi.crossserver.homes.HomesSyncService;
import org.xiaoziyi.crossserver.configcenter.RegisteredConfigDocument;
import org.xiaoziyi.crossserver.economy.EconomyService;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.player.PlayerLocationService;
import org.xiaoziyi.crossserver.player.PlayerLocationSnapshot;
import org.xiaoziyi.crossserver.session.SessionService;
import org.xiaoziyi.crossserver.sync.SyncNamespaceRegistry;
import org.xiaoziyi.crossserver.sync.SyncService;
import org.xiaoziyi.crossserver.teleport.TeleportInitiationResult;
import org.xiaoziyi.crossserver.teleport.TeleportTarget;
import org.xiaoziyi.crossserver.teleport.TransferAdminService;
import org.xiaoziyi.crossserver.teleport.TransferDiagnostics;
import org.xiaoziyi.crossserver.teleport.TransferHistoryEntry;
import org.xiaoziyi.crossserver.teleport.TeleportRequestService;
import org.xiaoziyi.crossserver.warp.WarpEntry;
import org.xiaoziyi.crossserver.warp.WarpService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class CrossServerApi {
	private final SyncService syncService;
	private final SyncNamespaceRegistry namespaceRegistry;
	private final SessionService sessionService;
	private final EconomyService economyService;
	private final ConfigCenterService configCenterService;
	private TransferAdminService transferAdminService;
	private TeleportApiFacade teleportApiFacade;
	private AuthService authService;
	private HomesSyncService homesSyncService;
	private WarpService warpService;
	private TeleportRequestService teleportRequestService;
	private SharedModuleConfigService sharedModuleConfigService;
	private RouteTableService routeTableService;
	private PluginConfiguration configuration;
	private NodeConfigSyncService nodeConfigSyncService;
	private PlayerLocationService playerLocationService;

	public CrossServerApi(
			SyncService syncService,
			SyncNamespaceRegistry namespaceRegistry,
			SessionService sessionService,
			EconomyService economyService
	) {
		this.syncService = syncService;
		this.namespaceRegistry = namespaceRegistry;
		this.sessionService = sessionService;
		this.economyService = economyService;
		this.configCenterService = new ConfigCenterService(this);
	}

	public void attachTransferAdminService(TransferAdminService transferAdminService) {
		this.transferAdminService = transferAdminService;
	}

	public void attachTeleportApiFacade(TeleportApiFacade teleportApiFacade) {
		this.teleportApiFacade = teleportApiFacade;
	}

	public void attachAuthService(AuthService authService) {
		this.authService = authService;
	}

	public void attachHomesSyncService(HomesSyncService homesSyncService) {
		this.homesSyncService = homesSyncService;
	}

	public void attachWarpService(WarpService warpService) {
		this.warpService = warpService;
	}

	public void attachTeleportRequestService(TeleportRequestService teleportRequestService) {
		this.teleportRequestService = teleportRequestService;
	}

	public void attachSharedModuleConfigService(SharedModuleConfigService sharedModuleConfigService) {
		this.sharedModuleConfigService = sharedModuleConfigService;
	}

	public void attachRouteTableService(RouteTableService routeTableService) {
		this.routeTableService = routeTableService;
	}

	public void attachConfiguration(PluginConfiguration configuration) {
		this.configuration = configuration;
	}

	public void attachNodeConfigSyncService(NodeConfigSyncService nodeConfigSyncService) {
		this.nodeConfigSyncService = nodeConfigSyncService;
	}

	public void attachPlayerLocationService(PlayerLocationService playerLocationService) {
		this.playerLocationService = playerLocationService;
	}

	public void registerNamespace(String namespace) {
		namespaceRegistry.registerNamespace(namespace);
	}

	public Optional<PlayerSnapshot> loadPlayerData(UUID playerId, String namespace) throws Exception {
		return syncService.loadPlayerData(playerId, namespace);
	}

	public PlayerSnapshot savePlayerData(UUID playerId, String namespace, String payload) throws Exception {
		return syncService.savePlayerData(playerId, namespace, payload);
	}

	public Optional<GlobalSnapshot> loadGlobalData(String namespace, String dataKey) throws Exception {
		return syncService.loadGlobalData(namespace, dataKey);
	}

	public GlobalSnapshot saveGlobalData(String namespace, String dataKey, String payload) throws Exception {
		return syncService.saveGlobalData(namespace, dataKey, payload);
	}

	public void registerSyncListener(String namespace, SyncService.SyncListener listener) {
		syncService.registerListener(namespace, listener);
	}

	public Runnable registerSyncListenerHandle(String namespace, SyncService.SyncListener listener) {
		return syncService.registerListenerHandle(namespace, listener);
	}

	public void registerSyncListener(SyncService.SyncListener listener) {
		syncService.registerListener(listener);
	}

	public Runnable registerSyncListenerHandle(SyncService.SyncListener listener) {
		return syncService.registerListenerHandle(listener);
	}

	public void registerConfigDocument(String namespace, String dataKey) {
		configCenterService.registerDocument(namespace, dataKey);
	}

	public void registerConfigDocument(String namespace, String dataKey, ConfigDocumentSchema schema) {
		configCenterService.registerDocument(namespace, dataKey, schema);
	}

	public Set<RegisteredConfigDocument> getRegisteredConfigDocuments() {
		return configCenterService.getRegisteredDocuments();
	}

	public Optional<ConfigEntry> loadConfigEntry(String namespace, String dataKey) throws Exception {
		return configCenterService.loadEntry(namespace, dataKey);
	}

	public Optional<ConfigDocument> loadConfigDocument(String namespace, String dataKey) throws Exception {
		return configCenterService.loadDocument(namespace, dataKey);
	}

	public List<Map<String, Object>> loadConfigDocumentHistory(String namespace, String dataKey) throws Exception {
		return configCenterService.loadDocumentHistory(namespace, dataKey);
	}

	public ConfigDocument saveConfigDocument(String namespace, String dataKey, ConfigDocumentUpdate update) throws Exception {
		return configCenterService.saveDocument(namespace, dataKey, update);
	}

	public ConfigDocument saveConfigDocument(String namespace, String dataKey, String payload) throws Exception {
		return configCenterService.saveDocument(namespace, dataKey, payload);
	}

	public Runnable registerConfigDocumentListener(String namespace, String dataKey, Consumer<ConfigChangeEvent> listener) {
		return configCenterService.registerDocumentListener(namespace, dataKey, listener);
	}

	public ConfigDocument rollbackConfigDocument(String namespace, String dataKey, long version, String actorName) throws Exception {
		Map<String, Object> historyItem = configCenterService.loadDocumentHistory(namespace, dataKey).stream()
				.filter(item -> item.get("version") instanceof Number number && number.longValue() == version)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("未找到指定历史版本: " + version));
		Object payloadValue = historyItem.get("payload");
		if (!(payloadValue instanceof String payload) || payload.isBlank()) {
			throw new IllegalArgumentException("历史版本 payload 为空");
		}
		int schemaVersion = historyItem.get("schemaVersion") instanceof Number number ? number.intValue() : 1;
		String actor = actorName == null || actorName.isBlank() ? "api" : actorName.trim();
		return saveConfigDocument(namespace, dataKey, new ConfigDocumentUpdate(payload, schemaVersion, actor, "api.rollback", "回滚配置文档到历史版本 " + version));
	}

	public void validateConfigDocument(String namespace, String dataKey, String payload) throws Exception {
		RegisteredConfigDocument registration = configCenterService.getRegisteredDocuments().stream()
				.filter(document -> namespace.equals(document.namespace()) && dataKey.equals(document.dataKey()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("未注册的配置文档: " + namespace + "/" + dataKey));
		ConfigDocumentSchemaValidator.validate(payload, registration.schema());
	}

	public boolean openPlayerSession(UUID playerId) throws Exception {
		return sessionService.openPlayerSession(playerId);
	}

	public void closePlayerSession(UUID playerId) {
		sessionService.closePlayerSession(playerId);
	}

	public SessionService sessionService() {
		return sessionService;
	}

	public EconomyService getEconomyService() {
		return economyService;
	}

	public List<HomeEntry> listHomes(UUID playerId) {
		if (homesSyncService == null) {
			return List.of();
		}
		return homesSyncService.listHomes(playerId);
	}

	public String getDefaultHome(UUID playerId) {
		return homesSyncService == null ? null : homesSyncService.getDefaultHome(playerId);
	}

	public List<WarpEntry> listWarps() {
		if (warpService == null) {
			return List.of();
		}
		return warpService.listWarps();
	}

	public boolean createTpaRequest(UUID senderId, String senderName, UUID receiverId, String receiverName, String senderServerId, TeleportRequestService.TpaType type) {
		if (teleportRequestService == null) {
			return false;
		}
		return teleportRequestService.submitRequest(senderId, senderName, receiverId, receiverName, senderServerId, type);
	}

	public Optional<TeleportRequestService.PendingRequest> getLatestTpaRequest(UUID receiverId) {
		if (teleportRequestService == null) {
			return Optional.empty();
		}
		return teleportRequestService.findLatestRequest(receiverId);
	}

	public Optional<TeleportRequestService.PendingRequest> consumeTpaRequest(UUID receiverId, UUID senderId) {
		if (teleportRequestService == null) {
			return Optional.empty();
		}
		return teleportRequestService.consumeRequest(receiverId, senderId);
	}

	public List<TeleportRequestService.PendingRequest> cancelOutgoingTpaRequests(UUID senderId) {
		if (teleportRequestService == null) {
			return List.of();
		}
		return teleportRequestService.removeRequestsBySender(senderId);
	}

	public TeleportRequestService.RequestStatus getTpaRequestStatus(UUID receiverId, UUID senderId) {
		if (teleportRequestService == null) {
			return TeleportRequestService.RequestStatus.NONE;
		}
		return teleportRequestService.getRequestStatus(receiverId, senderId);
	}

	public Optional<SharedModuleConfigSnapshot> loadSharedModules() {
		if (sharedModuleConfigService == null) {
			return Optional.empty();
		}
		return sharedModuleConfigService.loadSharedConfig();
	}

	public void saveSharedModules(SharedModuleConfigSnapshot snapshot, String actorName) throws Exception {
		if (sharedModuleConfigService == null) {
			throw new IllegalStateException("shared module config service not attached");
		}
		sharedModuleConfigService.saveSharedConfig(snapshot, actorName);
	}

	public Runnable registerSharedModulesListener(Consumer<ConfigChangeEvent> listener) {
		return registerConfigDocumentListener(SharedModuleConfigService.NAMESPACE, SharedModuleConfigService.DATA_KEY, listener);
	}

	public Map<String, String> loadSharedRoutes() {
		if (routeTableService == null) {
			return Map.of();
		}
		return routeTableService.loadSharedRoutes();
	}

	public void setSharedRoute(String serverId, String proxyTarget, String actorName) throws Exception {
		if (routeTableService == null) {
			throw new IllegalStateException("route table service not attached");
		}
		routeTableService.setSharedRoute(serverId, proxyTarget, actorName);
	}

	public boolean removeSharedRoute(String serverId, String actorName) throws Exception {
		if (routeTableService == null) {
			throw new IllegalStateException("route table service not attached");
		}
		return routeTableService.removeSharedRoute(serverId, actorName);
	}

	public Map<String, String> mergedRoutes() {
		if (routeTableService == null || configuration == null) {
			return Map.of();
		}
		return routeTableService.mergedRoutes(configuration);
	}

	public Runnable registerSharedRoutesListener(Consumer<ConfigChangeEvent> listener) {
		return registerConfigDocumentListener(RouteTableService.NAMESPACE, RouteTableService.DATA_KEY, listener);
	}

	public Optional<PlayerLocationSnapshot> getPlayerLocation(UUID playerId) {
		if (playerLocationService == null) {
			return Optional.empty();
		}
		return playerLocationService.getPlayerLocation(playerId);
	}

	public boolean isPlayerLocationFresh(PlayerLocationSnapshot snapshot) {
		return playerLocationService != null && playerLocationService.isFresh(snapshot);
	}

	public TeleportTarget toTeleportTarget(PlayerLocationSnapshot snapshot) {
		if (playerLocationService == null) {
			throw new IllegalStateException("player location service not attached");
		}
		return playerLocationService.toTeleportTarget(snapshot);
	}

	public TeleportInitiationResult requestTeleport(UUID playerId, TeleportTarget target, String causeRef) {
		if (teleportApiFacade == null) {
			return new TeleportInitiationResult(false, false, "§c跨服传送接口尚未初始化。", null);
		}
		Player player = Bukkit.getPlayer(playerId);
		if (player == null || !player.isOnline()) {
			return new TeleportInitiationResult(false, false, "§c玩家当前不在本服在线，无法发起跨服传送。", null);
		}
		return teleportApiFacade.requestTeleport(player, target, causeRef);
	}

	public Optional<TransferAdminService.TransferInspection> inspectTransfer(UUID playerId) throws Exception {
		if (transferAdminService == null) {
			return Optional.empty();
		}
		return Optional.of(transferAdminService.inspectPlayer(playerId));
	}

	public TransferDiagnostics inspectTransferDiagnostics(UUID playerId) throws Exception {
		if (transferAdminService == null) {
			throw new IllegalStateException("transfer admin service not attached");
		}
		return transferAdminService.loadDiagnostics(playerId, 5);
	}

	public List<TransferHistoryEntry> getTransferHistory(UUID playerId, int limit) throws Exception {
		if (transferAdminService == null) {
			return List.of();
		}
		return transferAdminService.getTransferHistory(playerId, limit);
	}

	public List<TransferHistoryEntry> getRecentTransferHistory(int limit) throws Exception {
		if (transferAdminService == null) {
			return List.of();
		}
		return transferAdminService.getRecentTransferHistory(limit);
	}

	public TransferAdminService.ClearResult clearTransfer(UUID playerId, String actorName) throws Exception {
		if (transferAdminService == null) {
			throw new IllegalStateException("transfer admin service not attached");
		}
		return transferAdminService.clearTransfer(playerId, actorName);
	}

	public void reconcileTransfer(UUID playerId, String playerName) throws Exception {
		if (teleportApiFacade instanceof TeleportControlFacade controlFacade) {
			controlFacade.reconcileTransfer(playerId, playerName);
			return;
		}
		throw new IllegalStateException("teleport control facade not attached");
	}

	public Map<String, Object> loadNodeConfigDetail(String serverId) {
		if (nodeConfigSyncService == null) {
			return Map.of();
		}
		return nodeConfigSyncService.loadNodeConfigDetail(serverId);
	}

	public Map<String, Object> loadNodeConfigs() {
		if (nodeConfigSyncService == null) {
			return Map.of();
		}
		return nodeConfigSyncService.loadClusterNodeConfigs();
	}

	public Map<String, Object> requestNodeConfigApply(String serverId, Map<String, Object> changes, String actorName) throws Exception {
		if (nodeConfigSyncService == null) {
			throw new IllegalStateException("node config sync service not attached");
		}
		return nodeConfigSyncService.requestApply(serverId, changes, actorName);
	}

	public boolean isAuthenticated(UUID playerId) {
		if (authService == null) {
			return true;
		}
		return authService.isAuthenticated(playerId);
	}

	public boolean shouldBlockUnauthenticatedPlayer(UUID playerId) {
		if (authService == null) {
			return false;
		}
		return authService.shouldBlock(playerId);
	}

	public Optional<AuthTicket> loadAuthTicket(UUID playerId) throws Exception {
		if (authService == null) {
			return Optional.empty();
		}
		AuthAdminInspection inspection = authService.inspectAuth(playerId);
		return Optional.ofNullable(inspection.ticket());
	}

	public AuthService.AuthAdminInspection inspectAuth(UUID playerId) throws Exception {
		if (authService == null) {
			throw new IllegalStateException("auth service not attached");
		}
		return authService.inspectAuth(playerId);
	}

	public String invalidateAuthTickets(UUID playerId, String actorName) throws Exception {
		if (authService == null) {
			throw new IllegalStateException("auth service not attached");
		}
		return authService.invalidateTickets(playerId, actorName);
	}

	public String forceReauthenticate(UUID playerId, String actorName) throws Exception {
		if (authService == null) {
			throw new IllegalStateException("auth service not attached");
		}
		return authService.forceReauthenticate(playerId, actorName);
	}

	public interface TeleportApiFacade {
		TeleportInitiationResult requestTeleport(Player player, TeleportTarget target, String causeRef);
	}

	public interface TeleportControlFacade extends TeleportApiFacade {
		void reconcileTransfer(UUID playerId, String playerName) throws Exception;
	}
}
