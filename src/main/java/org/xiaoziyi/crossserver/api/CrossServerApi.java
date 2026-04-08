package org.xiaoziyi.crossserver.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.xiaoziyi.crossserver.auth.AuthService;
import org.xiaoziyi.crossserver.configcenter.ConfigCenterService;
import org.xiaoziyi.crossserver.configcenter.ConfigChangeEvent;
import org.xiaoziyi.crossserver.configcenter.ConfigDocument;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentUpdate;
import org.xiaoziyi.crossserver.configcenter.ConfigEntry;
import org.xiaoziyi.crossserver.configcenter.RegisteredConfigDocument;
import org.xiaoziyi.crossserver.economy.EconomyService;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;
import org.xiaoziyi.crossserver.model.PlayerSnapshot;
import org.xiaoziyi.crossserver.session.SessionService;
import org.xiaoziyi.crossserver.sync.SyncNamespaceRegistry;
import org.xiaoziyi.crossserver.sync.SyncService;
import org.xiaoziyi.crossserver.teleport.TeleportInitiationResult;
import org.xiaoziyi.crossserver.teleport.TeleportTarget;
import org.xiaoziyi.crossserver.teleport.TransferAdminService;
import org.xiaoziyi.crossserver.teleport.TransferDiagnostics;
import org.xiaoziyi.crossserver.teleport.TransferHistoryEntry;

import java.util.List;
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

	public Set<RegisteredConfigDocument> getRegisteredConfigDocuments() {
		return configCenterService.getRegisteredDocuments();
	}

	public Optional<ConfigEntry> loadConfigEntry(String namespace, String dataKey) throws Exception {
		return configCenterService.loadEntry(namespace, dataKey);
	}

	public Optional<ConfigDocument> loadConfigDocument(String namespace, String dataKey) throws Exception {
		return configCenterService.loadDocument(namespace, dataKey);
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
}
