package org.xiaoziyi.crossserver.bootstrap;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.auth.AuthService;
import org.xiaoziyi.crossserver.command.AuthCommand;
import org.xiaoziyi.crossserver.command.CrossServerCommand;
import org.xiaoziyi.crossserver.command.EconomyCommand;
import org.xiaoziyi.crossserver.configcenter.NodeConfigSyncService;
import org.xiaoziyi.crossserver.command.HomesCommand;
import org.xiaoziyi.crossserver.command.TpaCommand;
import org.xiaoziyi.crossserver.command.WarpCommand;
import org.xiaoziyi.crossserver.config.ConfigLoader;
import org.xiaoziyi.crossserver.config.NodeLocalConfigService;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.config.RouteTableService;
import org.xiaoziyi.crossserver.config.SharedModuleConfigService;
import org.xiaoziyi.crossserver.economy.EconomyService;
import org.xiaoziyi.crossserver.economy.EconomyServiceImpl;
import org.xiaoziyi.crossserver.economy.VaultEconomyProvider;
import org.xiaoziyi.crossserver.homes.HomesSyncService;
import org.xiaoziyi.crossserver.inventory.PlayerInventorySyncService;
import org.xiaoziyi.crossserver.i18n.Texts;
import org.xiaoziyi.crossserver.listener.AuthListener;
import org.xiaoziyi.crossserver.listener.PlayerSessionListener;
import org.xiaoziyi.crossserver.listener.RouteChatInputListener;
import org.xiaoziyi.crossserver.messaging.MessagingProvider;
import org.xiaoziyi.crossserver.messaging.NoopMessagingProvider;
import org.xiaoziyi.crossserver.messaging.RedisMessagingProvider;
import org.xiaoziyi.crossserver.node.NodeIdentityGuardService;
import org.xiaoziyi.crossserver.node.NodeStatusService;
import org.xiaoziyi.crossserver.permission.NoopPermissionSyncAdapter;
import org.xiaoziyi.crossserver.permission.PlayerPermissionSyncService;
import org.xiaoziyi.crossserver.player.PlayerLocationService;
import org.xiaoziyi.crossserver.playerstate.PlayerStateSyncService;
import org.xiaoziyi.crossserver.session.SessionService;
import org.xiaoziyi.crossserver.storage.SqlStorageProvider;
import org.xiaoziyi.crossserver.storage.StorageProvider;
import org.xiaoziyi.crossserver.sync.SyncNamespaceRegistry;
import org.xiaoziyi.crossserver.sync.SyncService;
import org.xiaoziyi.crossserver.teleport.CrossServerTeleportService;
import org.xiaoziyi.crossserver.teleport.ProxyPluginMessageServerTransferGateway;
import org.xiaoziyi.crossserver.teleport.ServerTransferGateway;
import org.xiaoziyi.crossserver.teleport.TeleportArrivalListener;
import org.xiaoziyi.crossserver.teleport.TeleportRequestService;
import org.xiaoziyi.crossserver.teleport.TransferAdminService;
import org.xiaoziyi.crossserver.teleport.UnsupportedServerTransferGateway;
import org.xiaoziyi.crossserver.ui.HomesMenuService;
import org.xiaoziyi.crossserver.ui.CrossServerMainMenuService;
import org.xiaoziyi.crossserver.ui.MenuListener;
import org.xiaoziyi.crossserver.ui.RouteConfigMenuService;
import org.xiaoziyi.crossserver.ui.RouteEditSessionService;
import org.xiaoziyi.crossserver.ui.TransferAdminMenuService;
import org.xiaoziyi.crossserver.ui.WarpMenuService;
import org.xiaoziyi.crossserver.warp.WarpService;
import org.xiaoziyi.crossserver.web.WebPanelClusterService;
import org.xiaoziyi.crossserver.web.WebPanelClusterSnapshot;
import org.xiaoziyi.crossserver.web.WebPanelDataService;
import org.xiaoziyi.crossserver.web.WebPanelLogService;
import org.xiaoziyi.crossserver.web.WebPanelServer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CrossServerPlugin extends JavaPlugin {
	private PluginConfiguration localConfiguration;
	private PluginConfiguration configuration;
	private StorageProvider storageProvider;
	private MessagingProvider messagingProvider;
	private SyncNamespaceRegistry namespaceRegistry;
	private SyncService syncService;
	private SessionService sessionService;
	private EconomyService economyService;
	private NodeStatusService nodeStatusService;
	private PlayerInventorySyncService inventorySyncService;
	private PlayerStateSyncService playerStateSyncService;
	private PlayerPermissionSyncService playerPermissionSyncService;
	private PlayerLocationService playerLocationService;
	private HomesSyncService homesSyncService;
	private AuthService authService;
	private CrossServerTeleportService teleportService;
	private TeleportRequestService teleportRequestService;
	private TransferAdminService transferAdminService;
	private RouteTableService routeTableService;
	private SharedModuleConfigService sharedModuleConfigService;
	private WarpService warpService;
	private HomesMenuService homesMenuService;
	private CrossServerMainMenuService crossServerMainMenuService;
	private WarpMenuService warpMenuService;
	private TransferAdminMenuService transferAdminMenuService;
	private RouteConfigMenuService routeConfigMenuService;
	private RouteEditSessionService routeEditSessionService;
	private ServerTransferGateway transferGateway;
	private VaultEconomyProvider vaultEconomyProvider;
	private CrossServerApi api;
	private NodeConfigSyncService nodeConfigSyncService;
	private WebPanelClusterService webPanelClusterService;
	private WebPanelLogService webPanelLogService;
	private WebPanelServer webPanelServer;
	private PlayerSessionListener playerSessionListener;
	private AuthListener authListener;
	private TeleportArrivalListener teleportArrivalListener;
	private BukkitTask heartbeatTask;
	private BukkitTask nodeHeartbeatTask;
	private BukkitTask playerDataAutoSaveTask;
	private BukkitTask transferRecoveryTask;
	private BukkitTask webPanelHeartbeatTask;
	private boolean webPanelStartupHintLogged;
	private final AtomicBoolean reloading = new AtomicBoolean(false);
	private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
	private Texts texts;

	@Override
	public void onEnable() {
		try {
			initializePlugin();
			getLogger().info(texts.tr("plugin.started", configuration.server().id(), configuration.server().cluster()));
		} catch (Exception exception) {
			getLogger().severe((texts != null ? texts.tr("plugin.start_failed", exception.getMessage()) : "CrossServer 启动失败: " + exception.getMessage()));
			getServer().getPluginManager().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		shutdownPlugin();
	}

	public CrossServerApi getApi() {
		return api;
	}

	public CrossServerTeleportService getTeleportService() {
		return teleportService;
	}

	public RouteTableService getRouteTableService() {
		return routeTableService;
	}

	public SharedModuleConfigService getSharedModuleConfigService() {
		return sharedModuleConfigService;
	}

	public PluginConfiguration getLocalConfiguration() {
		return localConfiguration;
	}

	public RouteConfigMenuService getRouteConfigMenuService() {
		return routeConfigMenuService;
	}

	public CrossServerMainMenuService getCrossServerMainMenuService() {
		return crossServerMainMenuService;
	}

	public void reloadPlugin() throws Exception {
		if (!reloading.compareAndSet(false, true)) {
			throw new IllegalStateException("CrossServer 正在重载中");
		}
		try {
			performReloadCycle();
		} finally {
			reloading.set(false);
		}
	}

	public boolean requestReload(String actorName, String source) {
		if (!reloading.compareAndSet(false, true)) {
			return false;
		}
		String actor = actorName == null || actorName.isBlank() ? "unknown" : actorName.trim();
		String origin = source == null || source.isBlank() ? "unknown" : source.trim();
		Bukkit.getScheduler().runTask(this, () -> {
			try {
				getLogger().info(texts.tr("plugin.reload_requested", origin, actor));
				performReloadCycle();
				getLogger().info(texts.tr("plugin.reload_completed", origin, actor));
			} catch (Exception exception) {
				getLogger().severe(texts.tr("plugin.reload_failed", origin, actor, exception.getMessage()));
			} finally {
				reloading.set(false);
			}
		});
		return true;
	}

	private void performReloadCycle() throws Exception {
		shuttingDown.set(true);
		shutdownPlugin();
		initializePlugin();
		shuttingDown.set(false);
	}

	private void initializePlugin() throws Exception {
		PluginConfiguration baseConfiguration = new ConfigLoader().load(this);
		this.texts = new Texts(baseConfiguration.language());
		this.localConfiguration = baseConfiguration;
		this.configuration = baseConfiguration;
		this.storageProvider = new SqlStorageProvider(getLogger(), baseConfiguration.database());
		this.storageProvider.start();
		this.messagingProvider = baseConfiguration.messaging().enabled()
				? new RedisMessagingProvider(getLogger(), baseConfiguration.messaging())
				: new NoopMessagingProvider();
		this.messagingProvider.start();
		this.namespaceRegistry = new SyncNamespaceRegistry();
		namespaceRegistry.registerNamespace("core.player");
		namespaceRegistry.registerNamespace("core.global");
		namespaceRegistry.registerNamespace("session");
		namespaceRegistry.registerNamespace("economy");
		namespaceRegistry.registerNamespace(RouteTableService.NAMESPACE);
		namespaceRegistry.registerNamespace(HomesSyncService.NAMESPACE);
		namespaceRegistry.registerNamespace(AuthService.PROFILE_NAMESPACE);
		namespaceRegistry.registerNamespace(AuthService.TICKET_NAMESPACE);
		namespaceRegistry.registerNamespace(PlayerInventorySyncService.INVENTORY_NAMESPACE);
		namespaceRegistry.registerNamespace(PlayerInventorySyncService.ENDER_CHEST_NAMESPACE);
		namespaceRegistry.registerNamespace(PlayerStateSyncService.NAMESPACE);
		namespaceRegistry.registerNamespace(PlayerPermissionSyncService.NAMESPACE);
		namespaceRegistry.registerNamespace(CrossServerTeleportService.NAMESPACE);
		namespaceRegistry.registerNamespace(CrossServerTeleportService.ROLLBACK_INVENTORY_NAMESPACE);
		namespaceRegistry.registerNamespace(CrossServerTeleportService.ROLLBACK_ENDER_CHEST_NAMESPACE);
		namespaceRegistry.registerNamespace(CrossServerTeleportService.ROLLBACK_PLAYER_STATE_NAMESPACE);
		namespaceRegistry.registerNamespace(PlayerLocationService.NAMESPACE);
		namespaceRegistry.registerNamespace(TeleportRequestService.NAMESPACE);
		namespaceRegistry.registerNamespace(WarpService.NAMESPACE);
		this.syncService = new SyncService(getLogger(), storageProvider, messagingProvider, namespaceRegistry, baseConfiguration.server().id());
		this.sessionService = new SessionService(getLogger(), storageProvider, messagingProvider, baseConfiguration.server().id(), baseConfiguration.session());
		this.economyService = new EconomyServiceImpl(getLogger(), syncService, storageProvider);
		this.api = new CrossServerApi(syncService, namespaceRegistry, sessionService, economyService);
		this.routeTableService = new RouteTableService(getLogger(), api, baseConfiguration.server());
		this.sharedModuleConfigService = new SharedModuleConfigService(getLogger(), api, baseConfiguration.server());
		api.attachRouteTableService(routeTableService);
		api.attachSharedModuleConfigService(sharedModuleConfigService);
		this.configuration = sharedModuleConfigService.mergeInto(routeTableService.mergeInto(baseConfiguration));
		api.attachConfiguration(configuration);
		if (!reloading.get()) {
			new NodeIdentityGuardService(storageProvider, configuration.server(), configuration.node()).assertStartupAllowed();
		}
		this.nodeStatusService = new NodeStatusService(getLogger(), storageProvider, configuration.server(), configuration.node(), shuttingDown);
		this.inventorySyncService = new PlayerInventorySyncService(this, getLogger(), api);
		this.playerStateSyncService = new PlayerStateSyncService(this, getLogger(), api);
		if (configuration.modules().permissions()) {
			this.playerPermissionSyncService = new PlayerPermissionSyncService(this, getLogger(), api, new NoopPermissionSyncAdapter(this));
			getLogger().info("权限同步模块已启用，当前适配器: " + playerPermissionSyncService.adapterName());
		}
		this.playerLocationService = new PlayerLocationService(this, getLogger(), api, configuration.server().id());
		api.attachPlayerLocationService(playerLocationService);
		if (configuration.modules().auth()) {
			this.authService = new AuthService(this, getLogger(), api, configuration.server().id());
		}
		this.transferGateway = createTransferGateway();
		registerTransferGatewayChannels();
		this.teleportService = new CrossServerTeleportService(
				this,
				getLogger(),
				api,
				authService,
				inventorySyncService,
				playerStateSyncService,
				playerPermissionSyncService,
				storageProvider,
				null,
				transferGateway,
				configuration.server().id(),
				Duration.ofSeconds(configuration.teleport().handoffSeconds()),
				configuration.teleport().cooldownSeconds(),
				texts
		);
		if (configuration.modules().tpa()) {
			this.teleportRequestService = new TeleportRequestService(api, getLogger(), configuration.teleport().handoffSeconds());
			api.attachTeleportRequestService(teleportRequestService);
		}
		if (configuration.modules().homes()) {
			this.homesSyncService = new HomesSyncService(this, getLogger(), api, configuration.server().id(), teleportService, texts);
			teleportService.bindHomesSyncService(homesSyncService);
			this.homesMenuService = new HomesMenuService(this, homesSyncService);
			api.attachHomesSyncService(homesSyncService);
		}
		if (configuration.modules().warps()) {
			this.warpService = new WarpService(this, getLogger(), api, configuration.server().id(), teleportService, texts);
			this.warpMenuService = new WarpMenuService(this, warpService);
			api.attachWarpService(warpService);
		}
		if (configuration.modules().transferAdmin() || configuration.modules().tpa()) {
			this.transferAdminService = new TransferAdminService(api, storageProvider, sessionService);
			api.attachTransferAdminService(transferAdminService);
		}
		if (configuration.modules().transferAdmin()) {
			this.transferAdminMenuService = new TransferAdminMenuService(this, transferAdminService);
		}
		if (configuration.modules().routeConfig()) {
			this.routeEditSessionService = new RouteEditSessionService(this, routeTableService, configuration);
			this.routeConfigMenuService = new RouteConfigMenuService(this, routeTableService, configuration, routeEditSessionService);
		}
		this.crossServerMainMenuService = new CrossServerMainMenuService(this, homesMenuService, warpMenuService, routeConfigMenuService, transferAdminMenuService, texts);
		this.nodeConfigSyncService = new NodeConfigSyncService(this, api, configuration.server(), new NodeLocalConfigService(this));
		api.attachNodeConfigSyncService(nodeConfigSyncService);
		this.nodeConfigSyncService.publishLocalSnapshot(configuration);
		this.webPanelClusterService = new WebPanelClusterService(api, configuration.server(), configuration.webPanel());
		this.webPanelLogService = new WebPanelLogService(api, configuration.server(), getLogger());
		logWebPanelStartupHint();
		api.attachTeleportApiFacade(new CrossServerApi.TeleportControlFacade() {
			@Override
			public org.xiaoziyi.crossserver.teleport.TeleportInitiationResult requestTeleport(org.bukkit.entity.Player player, org.xiaoziyi.crossserver.teleport.TeleportTarget target, String causeRef) {
				return teleportService.requestTeleport(player, target, org.xiaoziyi.crossserver.teleport.TeleportCause.HOME, causeRef);
			}

			@Override
			public void reconcileTransfer(java.util.UUID playerId, String playerName) throws Exception {
				teleportService.reconcilePlayerTransfer(playerId, playerName);
			}
		});
		if (authService != null) {
			api.attachAuthService(authService);
		}
		this.messagingProvider.registerListener(message -> {
			if ("session".equalsIgnoreCase(message.namespace())) {
				sessionService.handleIncomingMessage(message);
				return;
			}
			syncService.handleIncomingMessage(message);
		});
		this.playerSessionListener = new PlayerSessionListener(this, sessionService, storageProvider, inventorySyncService, playerStateSyncService, playerPermissionSyncService, homesSyncService, teleportService, playerLocationService, configuration.session().kickMessage());
		if (authService != null) {
			this.authListener = new AuthListener(this, authService);
		}
		this.teleportArrivalListener = new TeleportArrivalListener(this, teleportService, configuration.teleport().arrivalCheckDelayTicks());
		Bukkit.getPluginManager().registerEvents(playerSessionListener, this);
		if (authListener != null) {
			Bukkit.getPluginManager().registerEvents(authListener, this);
		}
		Bukkit.getPluginManager().registerEvents(teleportArrivalListener, this);
		if (routeEditSessionService != null) {
			Bukkit.getPluginManager().registerEvents(new RouteChatInputListener(routeEditSessionService), this);
		}
		if (homesMenuService != null || warpMenuService != null || transferAdminMenuService != null || routeConfigMenuService != null || crossServerMainMenuService != null) {
			Bukkit.getPluginManager().registerEvents(new MenuListener(homesMenuService, warpMenuService, transferAdminMenuService, routeConfigMenuService, crossServerMainMenuService), this);
		}
		Bukkit.getServicesManager().register(CrossServerApi.class, api, this, ServicePriority.Normal);
		registerVaultProvider();
		registerCommand();
		startHeartbeat();
		startNodeHeartbeat();
		startPlayerDataAutoSave();
		startTransferRecovery();
		startWebPanelHeartbeat();
		refreshWebPanelLeadership();
	}

	private void shutdownPlugin() {
		shuttingDown.set(true);
		if (webPanelHeartbeatTask != null) {
			webPanelHeartbeatTask.cancel();
			webPanelHeartbeatTask = null;
		}
		if (webPanelClusterService != null) {
			try {
				webPanelClusterService.unregisterLocalMember();
			} catch (Exception exception) {
				getLogger().warning("注销 Web 面板节点失败: " + exception.getMessage());
			}
		}
		if (webPanelServer != null) {
			webPanelServer.close();
			webPanelServer = null;
		}
		if (webPanelLogService != null) {
			closeQuietly(webPanelLogService);
			webPanelLogService = null;
		}
		if (nodeConfigSyncService != null) {
			closeQuietly(nodeConfigSyncService);
			nodeConfigSyncService = null;
		}
		webPanelStartupHintLogged = false;
		if (heartbeatTask != null) {
			heartbeatTask.cancel();
			heartbeatTask = null;
		}
		if (nodeHeartbeatTask != null) {
			nodeHeartbeatTask.cancel();
			nodeHeartbeatTask = null;
		}
		if (playerDataAutoSaveTask != null) {
			playerDataAutoSaveTask.cancel();
			playerDataAutoSaveTask = null;
		}
		if (transferRecoveryTask != null) {
			transferRecoveryTask.cancel();
			transferRecoveryTask = null;
		}
		if (playerSessionListener != null) {
			HandlerList.unregisterAll(playerSessionListener);
			playerSessionListener = null;
		}
		if (authListener != null) {
			HandlerList.unregisterAll(authListener);
			authListener = null;
		}
		if (teleportArrivalListener != null) {
			HandlerList.unregisterAll(teleportArrivalListener);
			teleportArrivalListener = null;
		}
		if (teleportService != null) {
			teleportService.protectOnShutdown();
		}
		if (inventorySyncService != null) {
			Bukkit.getOnlinePlayers().forEach(inventorySyncService::savePlayerDataSync);
		}
		if (playerStateSyncService != null) {
			Bukkit.getOnlinePlayers().forEach(playerStateSyncService::savePlayerStateSync);
		}
		if (playerPermissionSyncService != null) {
			Bukkit.getOnlinePlayers().forEach(playerPermissionSyncService::savePermissionsSync);
			Bukkit.getOnlinePlayers().forEach(playerPermissionSyncService::clearPermissions);
		}
		if (homesSyncService != null) {
			Bukkit.getOnlinePlayers().forEach(player -> {
				homesSyncService.savePlayerHomesSync(player);
				homesSyncService.unloadPlayerHomes(player.getUniqueId());
			});
		}
		if (authService != null) {
			Bukkit.getOnlinePlayers().forEach(player -> authService.clearPlayer(player.getUniqueId()));
		}
		if (teleportRequestService != null) {
			Bukkit.getOnlinePlayers().forEach(player -> teleportRequestService.removeRequestsBySender(player.getUniqueId()));
			Bukkit.getOnlinePlayers().forEach(player -> teleportRequestService.removeRequestsByReceiver(player.getUniqueId()));
		}
		if (sessionService != null) {
			Bukkit.getOnlinePlayers().forEach(player -> sessionService.closePlayerSession(player.getUniqueId()));
		}
		if (vaultEconomyProvider != null) {
			Bukkit.getServicesManager().unregister(vaultEconomyProvider);
			vaultEconomyProvider = null;
		}
		if (api != null) {
			Bukkit.getServicesManager().unregister(CrossServerApi.class, api);
		}
		unregisterTransferGatewayChannels();
		awaitPendingAsyncTasks();
		if (nodeStatusService != null) {
			nodeStatusService.clearLocalNodeStatus();
		}
		closeQuietly(messagingProvider);
		closeQuietly(storageProvider);
		messagingProvider = null;
		storageProvider = null;
		namespaceRegistry = null;
		syncService = null;
		sessionService = null;
		economyService = null;
		nodeStatusService = null;
		inventorySyncService = null;
		playerStateSyncService = null;
		playerPermissionSyncService = null;
		playerLocationService = null;
		homesSyncService = null;
		authService = null;
		teleportService = null;
		teleportRequestService = null;
		warpService = null;
		transferAdminService = null;
		homesMenuService = null;
		crossServerMainMenuService = null;
		warpMenuService = null;
		transferAdminMenuService = null;
		routeConfigMenuService = null;
		routeEditSessionService = null;
		transferGateway = null;
		api = null;
		localConfiguration = null;
		configuration = null;
		routeTableService = null;
		sharedModuleConfigService = null;
		webPanelClusterService = null;
		shuttingDown.set(false);
	}

	private void awaitPendingAsyncTasks() {
		try {
			long deadline = System.currentTimeMillis() + 10000;
			while (System.currentTimeMillis() < deadline) {
				boolean hasAsyncTasks = false;
				for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
					if (task.getOwner().equals(this) && !task.isSync()) {
						hasAsyncTasks = true;
						break;
					}
				}
				if (!hasAsyncTasks) break;
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void registerCommand() {
		PluginCommand crossServerCommand = getCommand("crossserver");
		if (crossServerCommand != null) {
			CrossServerCommand executor = new CrossServerCommand(this, configuration, namespaceRegistry, sessionService, syncService, nodeStatusService, storageProvider, transferAdminService, transferAdminMenuService, routeConfigMenuService, texts);
			crossServerCommand.setExecutor(executor);
			crossServerCommand.setTabCompleter(executor);
		}
		PluginCommand economyCommand = getCommand("economy");
		if (economyCommand != null) {
			EconomyCommand executor = new EconomyCommand(this, api, storageProvider);
			economyCommand.setExecutor(executor);
			economyCommand.setTabCompleter(executor);
		}
		if (configuration.modules().homes() && homesSyncService != null && homesMenuService != null) {
			PluginCommand homeCommand = getCommand("home");
			PluginCommand homesCommand = getCommand("homes");
			PluginCommand setHomeCommand = getCommand("sethome");
			PluginCommand delHomeCommand = getCommand("delhome");
			PluginCommand setDefaultHomeCommand = getCommand("setdefaulthome");
			HomesCommand homesExecutor = new HomesCommand(homesSyncService, homesMenuService, texts);
			if (homeCommand != null) {
				homeCommand.setExecutor(homesExecutor);
				homeCommand.setTabCompleter(homesExecutor);
			}
			if (homesCommand != null) {
				homesCommand.setExecutor(homesExecutor);
				homesCommand.setTabCompleter(homesExecutor);
			}
			if (setHomeCommand != null) {
				setHomeCommand.setExecutor(homesExecutor);
				setHomeCommand.setTabCompleter(homesExecutor);
			}
			if (delHomeCommand != null) {
				delHomeCommand.setExecutor(homesExecutor);
				delHomeCommand.setTabCompleter(homesExecutor);
			}
			if (setDefaultHomeCommand != null) {
				setDefaultHomeCommand.setExecutor(homesExecutor);
				setDefaultHomeCommand.setTabCompleter(homesExecutor);
			}
		}
		if (configuration.modules().auth() && authService != null) {
			AuthCommand authExecutor = new AuthCommand(authService, texts, storageProvider);
			registerSimpleCommand("login", authExecutor);
			registerSimpleCommand("l", authExecutor);
			registerSimpleCommand("register", authExecutor);
			registerSimpleCommand("reg", authExecutor);
			registerSimpleCommand("changepassword", authExecutor);
		}
		if (configuration.modules().tpa() && transferAdminService != null && teleportRequestService != null && playerLocationService != null) {
			TpaCommand tpaExecutor = new TpaCommand(this, api, transferAdminService, teleportRequestService, playerLocationService, texts);
			registerSimpleCommand("tpa", tpaExecutor);
			registerSimpleCommand("tpahere", tpaExecutor);
			registerSimpleCommand("tpaccept", tpaExecutor);
			registerSimpleCommand("tpdeny", tpaExecutor);
			registerSimpleCommand("tpcancel", tpaExecutor);
		}
		if (configuration.modules().warps() && warpService != null && warpMenuService != null) {
			WarpCommand warpExecutor = new WarpCommand(warpService, warpMenuService, texts);
			registerSimpleCommand("warp", warpExecutor);
			registerSimpleCommand("setwarp", warpExecutor);
			registerSimpleCommand("delwarp", warpExecutor);
		}
	}

	private void registerSimpleCommand(String name, org.bukkit.command.TabExecutor executor) {
		PluginCommand command = getCommand(name);
		if (command == null) {
			return;
		}
		command.setExecutor(executor);
		command.setTabCompleter(executor);
	}

	private void registerVaultProvider() {
		if (!configuration.modules().economyBridge()) {
			getLogger().info("经济桥接模块已关闭，跳过 Vault 注册");
			return;
		}
		Bukkit.getScheduler().runTask(this, () -> {
			if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
				getLogger().info("未检测到 Vault，跳过经济桥接注册");
				return;
			}
			this.vaultEconomyProvider = new VaultEconomyProvider(this, economyService, storageProvider);
			Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, vaultEconomyProvider, this, ServicePriority.Highest);
			getLogger().info("已注册 Vault Economy Provider");
		});
	}

	private void startHeartbeat() {
		long period = Math.max(20L, configuration.session().heartbeatSeconds() * 20L);
		this.heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, sessionService::refreshLocalSessions, period, period);
	}

	private void startNodeHeartbeat() {
		long period = Math.max(20L, configuration.node().heartbeatSeconds() * 20L);
		this.nodeHeartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, nodeStatusService::heartbeat, 20L, period);
	}

	private void startPlayerDataAutoSave() {
		long period = Math.max(20L * 30L, configuration.session().heartbeatSeconds() * 20L * 2L);
		this.playerDataAutoSaveTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
			if (shuttingDown.get()) {
				return;
			}
			if (inventorySyncService != null) {
				Bukkit.getOnlinePlayers().forEach(inventorySyncService::savePlayerData);
			}
			if (playerStateSyncService != null) {
				Bukkit.getOnlinePlayers().forEach(playerStateSyncService::savePlayerState);
			}
			if (playerPermissionSyncService != null) {
				Bukkit.getOnlinePlayers().forEach(playerPermissionSyncService::savePermissions);
			}
			if (homesSyncService != null) {
				Bukkit.getOnlinePlayers().forEach(homesSyncService::savePlayerHomes);
			}
		}, period, period);
	}

	private void startTransferRecovery() {
		if (teleportService == null) {
			return;
		}
		this.transferRecoveryTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, teleportService::reconcilePendingTransfers, 20L * 10L, 20L * 10L);
	}

	private void logWebPanelStartupHint() {
		if (webPanelStartupHintLogged) {
			return;
		}
		webPanelStartupHintLogged = true;
		if (!configuration.webPanel().enabled()) {
			getLogger().info("Web 面板未启用。如需使用，请在 config.yml 中设置 web-panel.enabled: true");
			return;
		}
		String accessAddress = "http://" + configuration.webPanel().host() + ":" + configuration.webPanel().port();
		String token = configuration.webPanel().token();
		String masterServerId = configuration.webPanel().masterServerId();
		boolean localMaster = configuration.webPanel().isMasterServer(configuration.server().id());
		getLogger().info("Web 面板主控节点: " + masterServerId + (localMaster ? "（当前节点为主控）" : "（当前节点为受管节点）"));
		if (token == null || token.isBlank() || "change-this-token".equals(token)) {
			getLogger().warning("Web 面板已启用，但 token 仍为默认值或为空。请修改 web-panel.token 后再对外开放。访问地址: " + accessAddress);
		} else {
			getLogger().info("Web 面板已启用，候选监听地址: " + accessAddress + "，请求头需携带 X-CrossServer-Token");
		}
	}

	private void startWebPanelHeartbeat() {
		if (!configuration.webPanel().enabled() || webPanelClusterService == null) {
			return;
		}
		long period = Math.max(20L * 3L, configuration.webPanel().clusterHeartbeatSeconds() * 20L);
		this.webPanelHeartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::refreshWebPanelLeadership, 20L, period);
	}

	private void refreshWebPanelLeadership() {
		if (!configuration.webPanel().enabled() || webPanelClusterService == null) {
			return;
		}
		try {
			WebPanelClusterSnapshot snapshot = webPanelClusterService.heartbeat();
			if (webPanelLogService != null) {
				webPanelLogService.publishSnapshot();
			}
			if (nodeConfigSyncService != null && configuration != null) {
				nodeConfigSyncService.publishLocalSnapshot(configuration);
			}
			boolean shouldHost = webPanelClusterService.shouldHost(snapshot);
			if (shouldHost) {
				ensureWebPanelStarted();
			} else if (webPanelServer != null && webPanelServer.isRunning()) {
				webPanelServer.close();
				getLogger().info("当前节点不是 Web 面板主控节点，已停止本地面板监听。主控节点: " + snapshot.leaderServerId());
			}
		} catch (Exception exception) {
			getLogger().warning("同步 Web 面板主节点状态失败: " + exception.getMessage());
		}
	}

	private void ensureWebPanelStarted() throws Exception {
		if (webPanelServer != null && webPanelServer.isRunning()) {
			return;
		}
		WebPanelDataService dataService = new WebPanelDataService(
				this,
				configuration,
				namespaceRegistry,
				sessionService,
				syncService,
				nodeStatusService,
				routeTableService,
				sharedModuleConfigService,
				transferAdminService,
				storageProvider,
				api,
				webPanelClusterService,
				webPanelLogService,
				nodeConfigSyncService
		);
		this.webPanelServer = new WebPanelServer(getLogger(), configuration.webPanel(), dataService, webPanelLogService);
		this.webPanelServer.start();
		if (webPanelLogService != null) {
			webPanelLogService.captureStartupMessage("内置 Web 配置面板已启动: http://" + configuration.webPanel().host() + ":" + configuration.webPanel().port());
			webPanelLogService.publishSnapshot();
		}
	}

	private ServerTransferGateway createTransferGateway() {
		String type = configuration.teleport().gateway().type();
		if ("proxy-plugin-message".equalsIgnoreCase(type)) {
			return new ProxyPluginMessageServerTransferGateway(this, getLogger(), configuration.teleport().gateway());
		}
		getLogger().warning("未识别的 transfer gateway 类型: " + type + "，将使用 unsupported gateway");
		return new UnsupportedServerTransferGateway();
	}

	private void registerTransferGatewayChannels() {
		if (transferGateway instanceof ProxyPluginMessageServerTransferGateway proxyGateway) {
			Bukkit.getMessenger().registerOutgoingPluginChannel(this, proxyGateway.channel());
		}
	}

	private void unregisterTransferGatewayChannels() {
		if (transferGateway instanceof ProxyPluginMessageServerTransferGateway proxyGateway) {
			Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, proxyGateway.channel());
		}
	}

	private void closeQuietly(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception exception) {
			getLogger().warning("关闭资源失败: " + exception.getMessage());
		}
	}
}
