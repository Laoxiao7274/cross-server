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
import org.xiaoziyi.crossserver.command.HomesCommand;
import org.xiaoziyi.crossserver.config.ConfigLoader;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.config.RouteTableService;
import org.xiaoziyi.crossserver.economy.EconomyService;
import org.xiaoziyi.crossserver.economy.EconomyServiceImpl;
import org.xiaoziyi.crossserver.economy.VaultEconomyProvider;
import org.xiaoziyi.crossserver.homes.HomesSyncService;
import org.xiaoziyi.crossserver.inventory.PlayerInventorySyncService;
import org.xiaoziyi.crossserver.listener.AuthListener;
import org.xiaoziyi.crossserver.listener.PlayerSessionListener;
import org.xiaoziyi.crossserver.listener.RouteChatInputListener;
import org.xiaoziyi.crossserver.messaging.MessagingProvider;
import org.xiaoziyi.crossserver.messaging.NoopMessagingProvider;
import org.xiaoziyi.crossserver.messaging.RedisMessagingProvider;
import org.xiaoziyi.crossserver.node.NodeIdentityGuardService;
import org.xiaoziyi.crossserver.node.NodeStatusService;
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
import org.xiaoziyi.crossserver.teleport.TransferAdminService;
import org.xiaoziyi.crossserver.teleport.UnsupportedServerTransferGateway;
import org.xiaoziyi.crossserver.ui.HomesMenuService;
import org.xiaoziyi.crossserver.ui.MenuListener;
import org.xiaoziyi.crossserver.ui.RouteConfigMenuService;
import org.xiaoziyi.crossserver.ui.RouteEditSessionService;
import org.xiaoziyi.crossserver.ui.TransferAdminMenuService;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CrossServerPlugin extends JavaPlugin {
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
	private HomesSyncService homesSyncService;
	private AuthService authService;
	private CrossServerTeleportService teleportService;
	private TransferAdminService transferAdminService;
	private RouteTableService routeTableService;
	private HomesMenuService homesMenuService;
	private TransferAdminMenuService transferAdminMenuService;
	private RouteConfigMenuService routeConfigMenuService;
	private RouteEditSessionService routeEditSessionService;
	private ServerTransferGateway transferGateway;
	private VaultEconomyProvider vaultEconomyProvider;
	private CrossServerApi api;
	private PlayerSessionListener playerSessionListener;
	private AuthListener authListener;
	private TeleportArrivalListener teleportArrivalListener;
	private BukkitTask heartbeatTask;
	private BukkitTask nodeHeartbeatTask;
	private BukkitTask playerDataAutoSaveTask;
	private BukkitTask transferRecoveryTask;
	private final AtomicBoolean reloading = new AtomicBoolean(false);

	@Override
	public void onEnable() {
		try {
			initializePlugin();
			getLogger().info("CrossServer 已启动，节点: " + configuration.server().id() + " 集群: " + configuration.server().cluster());
		} catch (Exception exception) {
			getLogger().severe("CrossServer 启动失败: " + exception.getMessage());
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

	public RouteConfigMenuService getRouteConfigMenuService() {
		return routeConfigMenuService;
	}

	public void reloadPlugin() throws Exception {
		if (!reloading.compareAndSet(false, true)) {
			throw new IllegalStateException("CrossServer 正在重载中");
		}
		try {
			shutdownPlugin();
			initializePlugin();
		} finally {
			reloading.set(false);
		}
	}

	private void initializePlugin() throws Exception {
		PluginConfiguration baseConfiguration = new ConfigLoader().load(this);
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
		namespaceRegistry.registerNamespace(CrossServerTeleportService.NAMESPACE);
		this.syncService = new SyncService(getLogger(), storageProvider, messagingProvider, namespaceRegistry, baseConfiguration.server().id());
		this.sessionService = new SessionService(getLogger(), storageProvider, messagingProvider, baseConfiguration.server().id(), baseConfiguration.session());
		this.economyService = new EconomyServiceImpl(getLogger(), syncService, storageProvider);
		this.api = new CrossServerApi(syncService, namespaceRegistry, sessionService, economyService);
		this.routeTableService = new RouteTableService(getLogger(), api, baseConfiguration.server());
		this.configuration = routeTableService.mergeInto(baseConfiguration);
		if (!reloading.get()) {
			new NodeIdentityGuardService(storageProvider, configuration.server(), configuration.node()).assertStartupAllowed();
		}
		this.nodeStatusService = new NodeStatusService(getLogger(), storageProvider, configuration.server(), configuration.node());
		this.inventorySyncService = new PlayerInventorySyncService(this, getLogger(), api);
		this.playerStateSyncService = new PlayerStateSyncService(this, getLogger(), api);
		this.authService = new AuthService(this, getLogger(), api, configuration.server().id());
		this.transferGateway = createTransferGateway();
		registerTransferGatewayChannels();
		this.teleportService = new CrossServerTeleportService(
				this,
				getLogger(),
				api,
				authService,
				inventorySyncService,
				playerStateSyncService,
				storageProvider,
				null,
				transferGateway,
				configuration.server().id(),
				Duration.ofSeconds(configuration.teleport().handoffSeconds())
		);
		this.homesSyncService = new HomesSyncService(this, getLogger(), api, configuration.server().id(), teleportService);
		teleportService.bindHomesSyncService(homesSyncService);
		this.transferAdminService = new TransferAdminService(api, storageProvider, sessionService);
		this.homesMenuService = new HomesMenuService(this, homesSyncService);
		this.routeEditSessionService = new RouteEditSessionService(this, routeTableService, configuration);
		this.transferAdminMenuService = new TransferAdminMenuService(this, transferAdminService);
		this.routeConfigMenuService = new RouteConfigMenuService(this, routeTableService, configuration, routeEditSessionService);
		api.attachTransferAdminService(transferAdminService);
		api.attachTeleportApiFacade((player, target, causeRef) -> teleportService.requestTeleport(player, target, org.xiaoziyi.crossserver.teleport.TeleportCause.HOME, causeRef));
		api.attachAuthService(authService);
		this.messagingProvider.registerListener(message -> {
			if ("session".equalsIgnoreCase(message.namespace())) {
				sessionService.handleIncomingMessage(message);
				return;
			}
			syncService.handleIncomingMessage(message);
		});
		this.playerSessionListener = new PlayerSessionListener(this, sessionService, storageProvider, inventorySyncService, playerStateSyncService, homesSyncService, configuration.session().kickMessage());
		this.authListener = new AuthListener(this, authService);
		this.teleportArrivalListener = new TeleportArrivalListener(this, teleportService, configuration.teleport().arrivalCheckDelayTicks());
		Bukkit.getPluginManager().registerEvents(playerSessionListener, this);
		Bukkit.getPluginManager().registerEvents(authListener, this);
		Bukkit.getPluginManager().registerEvents(teleportArrivalListener, this);
		Bukkit.getPluginManager().registerEvents(new RouteChatInputListener(routeEditSessionService), this);
		Bukkit.getPluginManager().registerEvents(new MenuListener(homesMenuService, transferAdminMenuService, routeConfigMenuService), this);
		Bukkit.getServicesManager().register(CrossServerApi.class, api, this, ServicePriority.Normal);
		registerVaultProvider();
		registerCommand();
		startHeartbeat();
		startNodeHeartbeat();
		startPlayerDataAutoSave();
		startTransferRecovery();
	}

	private void shutdownPlugin() {
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
		if (inventorySyncService != null) {
			Bukkit.getOnlinePlayers().forEach(inventorySyncService::savePlayerData);
		}
		if (playerStateSyncService != null) {
			Bukkit.getOnlinePlayers().forEach(playerStateSyncService::savePlayerState);
		}
		if (homesSyncService != null) {
			Bukkit.getOnlinePlayers().forEach(player -> {
				homesSyncService.savePlayerHomes(player);
				homesSyncService.unloadPlayerHomes(player.getUniqueId());
			});
		}
		if (authService != null) {
			Bukkit.getOnlinePlayers().forEach(player -> authService.clearPlayer(player.getUniqueId()));
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
		homesSyncService = null;
		authService = null;
		teleportService = null;
		transferAdminService = null;
		homesMenuService = null;
		transferAdminMenuService = null;
		routeConfigMenuService = null;
		routeEditSessionService = null;
		transferGateway = null;
		api = null;
	}

	private void registerCommand() {
		PluginCommand crossServerCommand = getCommand("crossserver");
		if (crossServerCommand != null) {
			CrossServerCommand executor = new CrossServerCommand(this, configuration, namespaceRegistry, sessionService, syncService, nodeStatusService, storageProvider, transferAdminService, transferAdminMenuService, routeConfigMenuService);
			crossServerCommand.setExecutor(executor);
			crossServerCommand.setTabCompleter(executor);
		}
		PluginCommand economyCommand = getCommand("economy");
		if (economyCommand != null) {
			EconomyCommand executor = new EconomyCommand(this, api, storageProvider);
			economyCommand.setExecutor(executor);
			economyCommand.setTabCompleter(executor);
		}
		PluginCommand homeCommand = getCommand("home");
		PluginCommand homesCommand = getCommand("homes");
		PluginCommand setHomeCommand = getCommand("sethome");
		PluginCommand delHomeCommand = getCommand("delhome");
		PluginCommand setDefaultHomeCommand = getCommand("setdefaulthome");
		HomesCommand homesExecutor = new HomesCommand(homesSyncService, homesMenuService);
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
		AuthCommand authExecutor = new AuthCommand(authService);
		registerSimpleCommand("login", authExecutor);
		registerSimpleCommand("l", authExecutor);
		registerSimpleCommand("register", authExecutor);
		registerSimpleCommand("reg", authExecutor);
		registerSimpleCommand("changepassword", authExecutor);
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
			if (inventorySyncService == null || playerStateSyncService == null) {
				return;
			}
			Bukkit.getOnlinePlayers().forEach(player -> {
				inventorySyncService.savePlayerData(player);
				playerStateSyncService.savePlayerState(player);
			});
		}, period, period);
	}

	private void startTransferRecovery() {
		long period = Math.max(20L * 10L, configuration.teleport().arrivalCheckDelayTicks() * 2L);
		this.transferRecoveryTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			if (teleportService != null) {
				teleportService.reconcilePendingTransfers();
			}
		}, period, period);
	}

	private ServerTransferGateway createTransferGateway() {
		String type = configuration.teleport().gateway().type();
		if ("proxy-plugin-message".equalsIgnoreCase(type)) {
			return new ProxyPluginMessageServerTransferGateway(this, getLogger(), configuration.teleport().gateway());
		}
		if (!"unsupported".equalsIgnoreCase(type)) {
			getLogger().warning("未知 teleport gateway 类型: " + type + "，已回退到 unsupported");
		}
		return new UnsupportedServerTransferGateway();
	}

	private void registerTransferGatewayChannels() {
		if (transferGateway instanceof ProxyPluginMessageServerTransferGateway gateway) {
			Bukkit.getMessenger().registerOutgoingPluginChannel(this, gateway.channel());
			getLogger().info("已注册代理切服消息通道: " + gateway.channel());
		}
	}

	private void unregisterTransferGatewayChannels() {
		if (transferGateway instanceof ProxyPluginMessageServerTransferGateway gateway) {
			Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, gateway.channel());
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
