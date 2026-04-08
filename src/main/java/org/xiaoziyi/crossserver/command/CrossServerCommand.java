package org.xiaoziyi.crossserver.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xiaoziyi.crossserver.bootstrap.CrossServerPlugin;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.config.RouteTableService;
import org.xiaoziyi.crossserver.config.SharedModuleConfigService;
import org.xiaoziyi.crossserver.config.SharedModuleConfigSnapshot;
import org.xiaoziyi.crossserver.model.NodeStatusRecord;
import org.xiaoziyi.crossserver.node.NodeStatusService;
import org.xiaoziyi.crossserver.session.SessionService;
import org.xiaoziyi.crossserver.storage.StorageProvider;
import org.xiaoziyi.crossserver.sync.SyncNamespaceRegistry;
import org.xiaoziyi.crossserver.sync.SyncService;
import org.xiaoziyi.crossserver.teleport.TransferAdminService;
import org.xiaoziyi.crossserver.ui.RouteConfigMenuService;
import org.xiaoziyi.crossserver.ui.TransferAdminMenuService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CrossServerCommand implements CommandExecutor, TabCompleter {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final int NODES_PAGE_SIZE = 5;
	private static final String COMMAND_PERMISSION = "crossserver.command";
	private static final String STATUS_PERMISSION = "crossserver.status.view";
	private static final String NODES_PERMISSION = "crossserver.nodes.view";
	private static final String NODE_PERMISSION = "crossserver.node.view";
	private static final String TRANSFER_VIEW_PERMISSION = "crossserver.transfer.view";
	private static final String TRANSFER_CLEAR_PERMISSION = "crossserver.transfer.clear";
	private static final String TRANSFER_MENU_PERMISSION = "crossserver.transfer.menu";
	private static final String TRANSFER_RECONCILE_PERMISSION = "crossserver.transfer.reconcile";
	private static final String AUTH_ADMIN_PERMISSION = "crossserver.auth.admin";
	private static final String ROUTE_VIEW_PERMISSION = "crossserver.route.view";
	private static final String ROUTE_EDIT_PERMISSION = "crossserver.route.edit";
	private static final String MODULE_VIEW_PERMISSION = "crossserver.modules.view";
	private static final String MODULE_EDIT_PERMISSION = "crossserver.modules.edit";
	private static final String RELOAD_PERMISSION = "crossserver.reload";

	private final CrossServerPlugin plugin;
	private final PluginConfiguration configuration;
	private final SyncNamespaceRegistry namespaceRegistry;
	private final SessionService sessionService;
	private final SyncService syncService;
	private final NodeStatusService nodeStatusService;
	private final StorageProvider storageProvider;
	private final TransferAdminService transferAdminService;
	private final TransferAdminMenuService transferAdminMenuService;
	private final RouteConfigMenuService routeConfigMenuService;

	public CrossServerCommand(
			CrossServerPlugin plugin,
			PluginConfiguration configuration,
			SyncNamespaceRegistry namespaceRegistry,
			SessionService sessionService,
			SyncService syncService,
			NodeStatusService nodeStatusService,
			StorageProvider storageProvider,
			TransferAdminService transferAdminService,
			TransferAdminMenuService transferAdminMenuService,
			RouteConfigMenuService routeConfigMenuService
	) {
		this.plugin = plugin;
		this.configuration = configuration;
		this.namespaceRegistry = namespaceRegistry;
		this.sessionService = sessionService;
		this.syncService = syncService;
		this.nodeStatusService = nodeStatusService;
		this.storageProvider = storageProvider;
		this.transferAdminService = transferAdminService;
		this.transferAdminMenuService = transferAdminMenuService;
		this.routeConfigMenuService = routeConfigMenuService;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!sender.hasPermission(COMMAND_PERMISSION)) {
			sender.sendMessage("§c你没有权限执行此命令。");
			return true;
		}
		if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
			sendHelp(sender, label);
			return true;
		}
		if ("status".equalsIgnoreCase(args[0])) {
			if (!sender.hasPermission(STATUS_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看状态。");
				return true;
			}
			sender.sendMessage("§aCrossServer 状态");
			sender.sendMessage("§7节点: §f" + configuration.server().id());
			sender.sendMessage("§7集群: §f" + configuration.server().cluster());
			sender.sendMessage("§7命名空间数量: §f" + namespaceRegistry.getNamespaces().size());
			sender.sendMessage("§7本地会话数量: §f" + sessionService.getLocalSessionCount());
			sender.sendMessage("§7远端会话感知: §f" + sessionService.getRemoteSessionCount());
			sender.sendMessage("§7Prepared transfer 数量: §f" + sessionService.getPreparedTransferCount());
			sender.sendMessage("§7Teleport gateway: §f" + configuration.teleport().gateway().type());
			sender.sendMessage("§7Teleport handoff TTL: §f" + configuration.teleport().handoffSeconds() + " 秒");
			sender.sendMessage("§7Redis 同步: §f" + (configuration.messaging().enabled() ? "已启用" : "未启用"));
			sender.sendMessage("§7待处理失效: §f" + syncService.getPendingInvalidationCount());
			return true;
		}
		if ("transfer".equalsIgnoreCase(args[0])) {
			return handleTransferCommand(sender, label, args);
		}
		if ("auth".equalsIgnoreCase(args[0])) {
			return handleAuthCommand(sender, label, args);
		}
		if ("route".equalsIgnoreCase(args[0]) || "routes".equalsIgnoreCase(args[0])) {
			return handleRouteCommand(sender, label, args);
		}
		if ("modules".equalsIgnoreCase(args[0])) {
			return handleModulesCommand(sender, label, args);
		}
		if ("nodes".equalsIgnoreCase(args[0])) {
			if (!sender.hasPermission(NODES_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看节点列表。");
				return true;
			}
			try {
				List<NodeStatusRecord> records = nodeStatusService.listNodes();
				sender.sendMessage("§aCrossServer 节点列表");
				if (records.isEmpty()) {
					sender.sendMessage("§7当前没有节点心跳记录");
					return true;
				}
				int page = parsePage(args.length >= 2 ? args[1] : null);
				int totalPages = Math.max(1, (records.size() + NODES_PAGE_SIZE - 1) / NODES_PAGE_SIZE);
				if (page > totalPages) {
					page = totalPages;
				}
				int fromIndex = (page - 1) * NODES_PAGE_SIZE;
				int toIndex = Math.min(records.size(), fromIndex + NODES_PAGE_SIZE);
				List<NodeStatusRecord> pageRecords = records.subList(fromIndex, toIndex);
				long onlineCount = records.stream().filter(record -> "online".equals(record.status())).count();
				sender.sendMessage("§7总节点数: §f" + records.size() + " §7在线: §a" + onlineCount + " §7离线: §c" + (records.size() - onlineCount) + " §7页码: §f" + page + "/" + totalPages);
				for (NodeStatusRecord record : pageRecords) {
					sender.sendMessage(
							"§7- §f" + record.serverId()
									+ " §8[" + record.cluster() + "]"
									+ " §7状态: " + formatStatus(record.status())
									+ " §7最后心跳: §f" + TIME_FORMATTER.format(record.lastSeen())
									+ " §7更新距今: §f" + formatSince(record.updatedAt())
									+ " §7耗时: §f" + record.latencyMillis() + "ms"
					);
				}
				if (totalPages > 1) {
					sender.sendMessage("§8使用 /" + label + " nodes <页码> 查看更多");
				}
			} catch (Exception exception) {
				sender.sendMessage("§c查询节点状态失败: " + exception.getMessage());
			}
			return true;
		}
		if ("node".equalsIgnoreCase(args[0])) {
			if (!sender.hasPermission(NODE_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看节点详情。");
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage("§e用法: /" + label + " node <serverId>");
				return true;
			}
			try {
				NodeStatusRecord record = nodeStatusService.getNode(args[1]);
				if (record == null) {
					sender.sendMessage("§c未找到节点: " + args[1]);
					return true;
				}
				sender.sendMessage("§a节点详情");
				sender.sendMessage("§7节点: §f" + record.serverId());
				sender.sendMessage("§7集群: §f" + record.cluster());
				sender.sendMessage("§7状态: " + formatStatus(record.status()));
				sender.sendMessage("§7最后心跳: §f" + TIME_FORMATTER.format(record.lastSeen()));
				sender.sendMessage("§7记录更新时间: §f" + TIME_FORMATTER.format(record.updatedAt()) + " §8(" + formatSince(record.updatedAt()) + ")");
				sender.sendMessage("§7心跳写入耗时: §f" + record.latencyMillis() + "ms");
				sender.sendMessage("§7离线判定阈值: §f" + configuration.node().offlineSeconds() + " 秒");
			} catch (Exception exception) {
				sender.sendMessage("§c查询节点详情失败: " + exception.getMessage());
			}
			return true;
		}
		if ("reload".equalsIgnoreCase(args[0])) {
			if (!sender.hasPermission(RELOAD_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行此命令。");
				return true;
			}
			boolean accepted = plugin.requestReload(sender.getName(), "command:/crossserver reload");
			if (!accepted) {
				sender.sendMessage("§cCrossServer 正在重载中，请稍后再试。");
				return true;
			}
			sender.sendMessage("§e已接受重载请求，CrossServer 即将开始重载...");
			return true;
		}
		sendHelp(sender, label);
		return true;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (!sender.hasPermission(COMMAND_PERMISSION)) {
			return List.of();
		}
		if (args.length == 1) {
			List<String> result = new ArrayList<>();
			result.add("help");
			if (sender.hasPermission(STATUS_PERMISSION)) {
				result.add("status");
			}
			if (sender.hasPermission(NODES_PERMISSION)) {
				result.add("nodes");
			}
			if (sender.hasPermission(NODE_PERMISSION)) {
				result.add("node");
			}
			if (canUseTransferCommands(sender)) {
				result.add("transfer");
			}
			if (sender.hasPermission(AUTH_ADMIN_PERMISSION)) {
				result.add("auth");
			}
			if (sender.hasPermission(ROUTE_VIEW_PERMISSION) || sender.hasPermission(ROUTE_EDIT_PERMISSION)) {
				result.add("route");
				result.add("routes");
			}
			if (sender.hasPermission(MODULE_VIEW_PERMISSION) || sender.hasPermission(MODULE_EDIT_PERMISSION)) {
				result.add("modules");
			}
			if (sender.hasPermission(RELOAD_PERMISSION)) {
				result.add("reload");
			}
			return result;
		}
		if ("transfer".equalsIgnoreCase(args[0]) && args.length == 2) {
			return visibleTransferActions(sender);
		}
		if ("auth".equalsIgnoreCase(args[0]) && args.length == 2 && sender.hasPermission(AUTH_ADMIN_PERMISSION)) {
			return List.of("inspect", "invalidate", "forcereauth");
		}
		if (("route".equalsIgnoreCase(args[0]) || "routes".equalsIgnoreCase(args[0])) && args.length == 2) {
			List<String> result = new ArrayList<>();
			if (sender.hasPermission(ROUTE_VIEW_PERMISSION)) {
				result.add("list");
				result.add("menu");
			}
			if (sender.hasPermission(ROUTE_EDIT_PERMISSION)) {
				result.add("set");
				result.add("remove");
			}
			return result;
		}
		if ("modules".equalsIgnoreCase(args[0]) && args.length == 2) {
			List<String> result = new ArrayList<>();
			if (sender.hasPermission(MODULE_VIEW_PERMISSION)) {
				result.add("list");
			}
			if (sender.hasPermission(MODULE_EDIT_PERMISSION)) {
				result.add("set");
				result.add("clear");
			}
			return result;
		}
		if ("modules".equalsIgnoreCase(args[0]) && args.length == 3) {
			return List.of("auth", "homes", "warps", "tpa", "route-config", "transfer-admin", "economy-bridge", "permissions");
		}
		if ("modules".equalsIgnoreCase(args[0]) && args.length == 4 && "set".equalsIgnoreCase(args[1])) {
			return List.of("true", "false");
		}
		return List.of();
	}

	private boolean handleRouteCommand(CommandSender sender, String label, String[] args) {
		if (args.length < 2 || "help".equalsIgnoreCase(args[1])) {
			sendRouteHelp(sender, label);
			return true;
		}
		String action = args[1];
		RouteTableService routeTableService = plugin.getRouteTableService();
		if (routeTableService == null) {
			sender.sendMessage("§c共享路由服务尚未初始化。");
			return true;
		}
		if ("list".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(ROUTE_VIEW_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看共享路由。");
				return true;
			}
			Map<String, String> sharedRoutes = routeTableService.loadSharedRoutes();
			Map<String, String> mergedRoutes = routeTableService.mergedRoutes(configuration);
			sender.sendMessage("§a共享路由列表");
			if (mergedRoutes.isEmpty()) {
				sender.sendMessage("§7当前没有可用路由。请先检查本地 config 或共享路由配置。");
				return true;
			}
			for (Map.Entry<String, String> entry : mergedRoutes.entrySet()) {
				boolean shared = sharedRoutes.containsKey(entry.getKey());
				sender.sendMessage("§7- §f" + entry.getKey() + " §8-> §f" + entry.getValue() + " §8[" + (shared ? "shared" : "local") + "]");
			}
			sender.sendMessage("§8修改共享路由后请执行 /" + label + " reload 生效");
			return true;
		}
		if ("menu".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(ROUTE_VIEW_PERMISSION)) {
				sender.sendMessage("§c你没有权限打开路由菜单。");
				return true;
			}
			if (routeConfigMenuService == null) {
				sender.sendMessage("§c路由管理模块当前已关闭。");
				return true;
			}
			if (!(sender instanceof org.bukkit.entity.Player player)) {
				sender.sendMessage("§c该命令只能由玩家执行。");
				return true;
			}
			routeConfigMenuService.openMenu(player);
			return true;
		}
		if ("set".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(ROUTE_EDIT_PERMISSION)) {
				sender.sendMessage("§c你没有权限修改共享路由。");
				return true;
			}
			if (args.length < 4) {
				sender.sendMessage("§e用法: /" + label + " route set <serverId> <proxyServer>");
				return true;
			}
			try {
				routeTableService.setSharedRoute(args[2], args[3], sender.getName());
				sender.sendMessage("§a已保存共享路由: §f" + args[2] + " §8-> §f" + args[3]);
				sender.sendMessage("§e请执行 /" + label + " reload 让本节点立即生效。");
			} catch (Exception exception) {
				sender.sendMessage("§c保存共享路由失败: " + exception.getMessage());
			}
			return true;
		}
		if ("remove".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(ROUTE_EDIT_PERMISSION)) {
				sender.sendMessage("§c你没有权限删除共享路由。");
				return true;
			}
			if (args.length < 3) {
				sender.sendMessage("§e用法: /" + label + " route remove <serverId>");
				return true;
			}
			try {
				boolean removed = routeTableService.removeSharedRoute(args[2], sender.getName());
				if (!removed) {
					sender.sendMessage("§e共享路由中不存在该 serverId: §f" + args[2]);
					return true;
				}
				sender.sendMessage("§a已移除共享路由: §f" + args[2]);
				sender.sendMessage("§e请执行 /" + label + " reload 让本节点立即生效。");
			} catch (Exception exception) {
				sender.sendMessage("§c移除共享路由失败: " + exception.getMessage());
			}
			return true;
		}
		sendRouteHelp(sender, label);
		return true;
	}

	private boolean handleModulesCommand(CommandSender sender, String label, String[] args) {
		if (args.length < 2 || "help".equalsIgnoreCase(args[1])) {
			sendModulesHelp(sender, label);
			return true;
		}
		SharedModuleConfigService moduleService = plugin.getSharedModuleConfigService();
		if (moduleService == null) {
			sender.sendMessage("§c共享模块配置服务尚未初始化。");
			return true;
		}
		String action = args[1];
		if ("list".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(MODULE_VIEW_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看模块开关。");
				return true;
			}
			Optional<SharedModuleConfigSnapshot> shared = moduleService.loadSharedConfig();
			sender.sendMessage("§a模块开关列表");
			sendModuleLine(sender, "auth", configuration.modules().auth(), shared.map(SharedModuleConfigSnapshot::auth).orElse(null));
			sendModuleLine(sender, "homes", configuration.modules().homes(), shared.map(SharedModuleConfigSnapshot::homes).orElse(null));
			sendModuleLine(sender, "warps", configuration.modules().warps(), shared.map(SharedModuleConfigSnapshot::warps).orElse(null));
			sendModuleLine(sender, "tpa", configuration.modules().tpa(), shared.map(SharedModuleConfigSnapshot::tpa).orElse(null));
			sendModuleLine(sender, "route-config", configuration.modules().routeConfig(), shared.map(SharedModuleConfigSnapshot::routeConfig).orElse(null));
			sendModuleLine(sender, "transfer-admin", configuration.modules().transferAdmin(), shared.map(SharedModuleConfigSnapshot::transferAdmin).orElse(null));
			sendModuleLine(sender, "economy-bridge", configuration.modules().economyBridge(), shared.map(SharedModuleConfigSnapshot::economyBridge).orElse(null));
			sendModuleLine(sender, "permissions", configuration.modules().permissions(), shared.map(SharedModuleConfigSnapshot::permissions).orElse(null));
			sender.sendMessage("§8local = 本地默认，shared = 共享覆盖；修改后需 /" + label + " reload 生效");
			return true;
		}
		if ("set".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(MODULE_EDIT_PERMISSION)) {
				sender.sendMessage("§c你没有权限修改模块开关。");
				return true;
			}
			if (args.length < 4) {
				sender.sendMessage("§e用法: /" + label + " modules set <module> <true|false>");
				return true;
			}
			String module = args[2];
			boolean enabled = Boolean.parseBoolean(args[3]);
			try {
				SharedModuleConfigSnapshot next = mergeSharedSnapshot(moduleService.loadSharedConfig().orElse(null), module, enabled, false);
				moduleService.saveSharedConfig(next, sender.getName());
				sender.sendMessage("§a已保存共享模块开关: §f" + module + " §8= §f" + enabled);
				sender.sendMessage("§e请执行 /" + label + " reload 让本节点立即生效。");
			} catch (IllegalArgumentException exception) {
				sender.sendMessage("§c" + exception.getMessage());
			} catch (Exception exception) {
				sender.sendMessage("§c保存共享模块开关失败: " + exception.getMessage());
			}
			return true;
		}
		if ("clear".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(MODULE_EDIT_PERMISSION)) {
				sender.sendMessage("§c你没有权限清除模块共享覆盖。");
				return true;
			}
			if (args.length < 3) {
				sender.sendMessage("§e用法: /" + label + " modules clear <module>");
				return true;
			}
			try {
				SharedModuleConfigSnapshot next = mergeSharedSnapshot(moduleService.loadSharedConfig().orElse(null), args[2], false, true);
				moduleService.saveSharedConfig(next, sender.getName());
				sender.sendMessage("§a已清除共享模块覆盖: §f" + args[2]);
				sender.sendMessage("§e请执行 /" + label + " reload 让本节点立即生效。");
			} catch (IllegalArgumentException exception) {
				sender.sendMessage("§c" + exception.getMessage());
			} catch (Exception exception) {
				sender.sendMessage("§c清除共享模块开关失败: " + exception.getMessage());
			}
			return true;
		}
		sendModulesHelp(sender, label);
		return true;
	}

	private boolean handleTransferCommand(CommandSender sender, String label, String[] args) {
		if (args.length < 2 || "help".equalsIgnoreCase(args[1])) {
			sendTransferHelp(sender, label);
			return true;
		}
		if (transferAdminService == null || transferAdminMenuService == null || plugin.getTeleportService() == null) {
			sender.sendMessage("§cTransfer 管理模块当前已关闭。");
			return true;
		}
		String action = args[1];
		String playerName = null;
		if ("info".equalsIgnoreCase(action) || "clear".equalsIgnoreCase(action) || "menu".equalsIgnoreCase(action) || "history".equalsIgnoreCase(action) || "reconcile".equalsIgnoreCase(action)) {
			if (args.length < 3) {
				sendTransferHelp(sender, label);
				return true;
			}
			playerName = args[2];
		} else if ("recent".equalsIgnoreCase(action)) {
			playerName = null;
		} else if (args.length == 2) {
			action = "info";
			playerName = args[1];
		} else {
			sendTransferHelp(sender, label);
			return true;
		}
		if ("info".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(TRANSFER_VIEW_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看 transfer 诊断。");
				return true;
			}
			try {
				Optional<TransferAdminService.TransferInspection> inspection = transferAdminService.inspectPlayer(playerName);
				if (inspection.isEmpty()) {
					sender.sendMessage("§c未找到玩家: " + playerName);
					return true;
				}
				transferAdminMenuService.sendInspection(sender, inspection.get());
			} catch (Exception exception) {
				sender.sendMessage("§c查询 transfer 诊断失败: " + exception.getMessage());
			}
			return true;
		}
		if ("clear".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(TRANSFER_CLEAR_PERMISSION)) {
				sender.sendMessage("§c你没有权限清理 transfer 状态。");
				return true;
			}
			try {
				Optional<TransferAdminService.TransferInspection> inspection = transferAdminService.inspectPlayer(playerName);
				if (inspection.isEmpty()) {
					sender.sendMessage("§c未找到玩家: " + playerName);
					return true;
				}
				TransferAdminService.ClearResult result = transferAdminService.clearTransfer(inspection.get().playerId(), sender.getName());
				sender.sendMessage(result.message());
				transferAdminMenuService.sendInspection(sender, result.inspection());
			} catch (Exception exception) {
				sender.sendMessage("§c清理 transfer 状态失败: " + exception.getMessage());
			}
			return true;
		}
		if ("recent".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(TRANSFER_MENU_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看 transfer recent。\n§8需要 crossserver.transfer.menu");
				return true;
			}
			if (!(sender instanceof org.bukkit.entity.Player player)) {
				sender.sendMessage("§c该命令只能由玩家执行。\n§7可改用 /" + label + " transfer history <player>");
				return true;
			}
			transferAdminMenuService.openRecentMenu(player, parsePage(args.length >= 3 ? args[2] : null));
			return true;
		}
		if ("reconcile".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(TRANSFER_RECONCILE_PERMISSION)) {
				sender.sendMessage("§c你没有权限执行 transfer reconcile。\n§8需要 crossserver.transfer.reconcile");
				return true;
			}
			try {
				Optional<TransferAdminService.TransferInspection> inspection = transferAdminService.inspectPlayer(playerName);
				if (inspection.isEmpty()) {
					sender.sendMessage("§c未找到玩家: " + playerName);
					return true;
				}
				plugin.getTeleportService().reconcilePlayerTransfer(inspection.get().playerId(), inspection.get().playerName());
				sender.sendMessage("§a已触发 transfer reconcile: §f" + inspection.get().playerName());
				transferAdminMenuService.sendInspection(sender, transferAdminService.inspectPlayer(inspection.get().playerId()));
			} catch (Exception exception) {
				sender.sendMessage("§c执行 transfer reconcile 失败: " + exception.getMessage());
			}
			return true;
		}
		if ("history".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(TRANSFER_VIEW_PERMISSION)) {
				sender.sendMessage("§c你没有权限查看 transfer 历史。");
				return true;
			}
			try {
				Optional<TransferAdminService.TransferInspection> inspection = transferAdminService.inspectPlayer(playerName);
				if (inspection.isEmpty()) {
					sender.sendMessage("§c未找到玩家: " + playerName);
					return true;
				}
				sender.sendMessage("§aTransfer 历史 - §f" + inspection.get().playerName());
				for (var entry : transferAdminService.getTransferHistory(inspection.get().playerId(), 10)) {
					sender.sendMessage("§7- §f" + entry.createdAt() + " §8[" + entry.eventType() + "] §7" + (entry.status() == null ? "-" : entry.status()) + " §f" + (entry.detail() == null ? "" : entry.detail()));
				}
			} catch (Exception exception) {
				sender.sendMessage("§c查询 transfer 历史失败: " + exception.getMessage());
			}
			return true;
		}
		if ("menu".equalsIgnoreCase(action)) {
			if (!sender.hasPermission(TRANSFER_MENU_PERMISSION)) {
				sender.sendMessage("§c你没有权限打开 transfer 菜单。");
				return true;
			}
			if (!(sender instanceof org.bukkit.entity.Player player)) {
				sender.sendMessage("§c该命令只能由玩家执行。");
				return true;
			}
			try {
				Optional<TransferAdminService.TransferInspection> inspection = transferAdminService.inspectPlayer(playerName);
				if (inspection.isEmpty()) {
					sender.sendMessage("§c未找到玩家: " + playerName);
					return true;
				}
				transferAdminMenuService.openMenu(player, inspection.get().playerId());
			} catch (Exception exception) {
				sender.sendMessage("§c打开 transfer 菜单失败: " + exception.getMessage());
			}
			return true;
		}
		sendTransferHelp(sender, label);
		return true;
	}

	private void sendHelp(CommandSender sender, String label) {
		sender.sendMessage("§aCrossServer 命令帮助");
		sender.sendMessage("§7使用 §f/" + label + " <子命令> §7执行对应功能");
		sender.sendMessage("§7基础命令:");
		sendHelpLine(sender, label, "help", "显示此帮助");
		if (sender.hasPermission(STATUS_PERMISSION)) {
			sendHelpLine(sender, label, "status", "查看当前节点与同步状态");
		}
		if (sender.hasPermission(NODES_PERMISSION)) {
			sendHelpLine(sender, label, "nodes [page]", "分页查看节点列表");
		}
		if (sender.hasPermission(NODE_PERMISSION)) {
			sendHelpLine(sender, label, "node <serverId>", "查看单个节点详情");
		}
		if (canUseTransferCommands(sender)) {
			sender.sendMessage("§7Transfer 命令:");
			sendTransferHelpLines(sender, label);
		}
		if (sender.hasPermission(AUTH_ADMIN_PERMISSION)) {
			sender.sendMessage("§7Auth 管理:");
			sendHelpLine(sender, label, "auth inspect <player>", "查看 auth 运行时与快照状态");
			sendHelpLine(sender, label, "auth invalidate <player>", "使玩家跨服 ticket 失效");
			sendHelpLine(sender, label, "auth forcereauth <player>", "强制玩家重新认证");
		}
		if (sender.hasPermission(ROUTE_VIEW_PERMISSION) || sender.hasPermission(ROUTE_EDIT_PERMISSION)) {
			sender.sendMessage("§7路由管理:");
			sendRouteHelpLines(sender, label);
		}
		if (sender.hasPermission(MODULE_VIEW_PERMISSION) || sender.hasPermission(MODULE_EDIT_PERMISSION)) {
			sender.sendMessage("§7模块配置:");
			sendModulesHelpLines(sender, label);
		}
		if (sender.hasPermission(RELOAD_PERMISSION)) {
			sender.sendMessage("§7其他:");
			sendHelpLine(sender, label, "reload", "热重载配置与内部服务");
		}
	}

	private void sendTransferHelp(CommandSender sender, String label) {
		sender.sendMessage("§aCrossServer Transfer 帮助");
		if (!canUseTransferCommands(sender)) {
			sender.sendMessage("§7你当前没有可用的 transfer 子命令。");
			return;
		}
		sendTransferHelpLines(sender, label);
	}

	private void sendTransferHelpLines(CommandSender sender, String label) {
		if (sender.hasPermission(TRANSFER_VIEW_PERMISSION)) {
			sendHelpLine(sender, label, "transfer <player>", "按玩家名快速查看 transfer 诊断");
			sendHelpLine(sender, label, "transfer info <player>", "查看 transfer 诊断详情");
			sendHelpLine(sender, label, "transfer history <player>", "查看最近 transfer 历史");
		}
		if (sender.hasPermission(TRANSFER_MENU_PERMISSION)) {
			sendHelpLine(sender, label, "transfer menu <player>", "打开指定玩家的 transfer 菜单");
			sendHelpLine(sender, label, "transfer recent [page]", "打开 recent transfer 管理菜单");
		}
		if (sender.hasPermission(TRANSFER_RECONCILE_PERMISSION)) {
			sendHelpLine(sender, label, "transfer reconcile <player>", "触发一次保守 reconcile 修补");
		}
		if (sender.hasPermission(TRANSFER_CLEAR_PERMISSION)) {
			sendHelpLine(sender, label, "transfer clear <player>", "清理 handoff 与 prepared transfer 状态");
		}
	}

	private void sendRouteHelp(CommandSender sender, String label) {
		sender.sendMessage("§aCrossServer 路由帮助");
		if (!sender.hasPermission(ROUTE_VIEW_PERMISSION) && !sender.hasPermission(ROUTE_EDIT_PERMISSION)) {
			sender.sendMessage("§7你当前没有可用的路由子命令。");
			return;
		}
		sendRouteHelpLines(sender, label);
	}

	private void sendRouteHelpLines(CommandSender sender, String label) {
		if (sender.hasPermission(ROUTE_VIEW_PERMISSION)) {
			sendHelpLine(sender, label, "route list", "查看共享路由与本地合并结果");
			sendHelpLine(sender, label, "route menu", "打开路由管理菜单");
		}
		if (sender.hasPermission(ROUTE_EDIT_PERMISSION)) {
			sendHelpLine(sender, label, "route set <serverId> <proxyServer>", "设置共享路由覆盖");
			sendHelpLine(sender, label, "route remove <serverId>", "移除共享路由覆盖");
		}
	}

	private void sendModulesHelp(CommandSender sender, String label) {
		sender.sendMessage("§aCrossServer 模块配置帮助");
		if (!sender.hasPermission(MODULE_VIEW_PERMISSION) && !sender.hasPermission(MODULE_EDIT_PERMISSION)) {
			sender.sendMessage("§7你当前没有可用的模块配置子命令。");
			return;
		}
		sendModulesHelpLines(sender, label);
	}

	private void sendModulesHelpLines(CommandSender sender, String label) {
		if (sender.hasPermission(MODULE_VIEW_PERMISSION)) {
			sendHelpLine(sender, label, "modules list", "查看模块本地默认、共享覆盖与最终有效值");
		}
		if (sender.hasPermission(MODULE_EDIT_PERMISSION)) {
			sendHelpLine(sender, label, "modules set <module> <true|false>", "设置共享模块开关覆盖");
			sendHelpLine(sender, label, "modules clear <module>", "移除共享模块开关覆盖");
		}
	}

	private void sendHelpLine(CommandSender sender, String label, String usage, String description) {
		sender.sendMessage("§e/" + label + " " + usage + " §8- §7" + description);
	}

	private void sendModuleLine(CommandSender sender, String module, boolean effectiveValue, Boolean sharedOverride) {
		boolean localDefault = localDefault(module);
		String source = sharedOverride == null ? "local" : "shared";
		sender.sendMessage("§7- §f" + module
				+ " §7effective: " + boolText(effectiveValue)
				+ " §7local: " + boolText(localDefault)
				+ " §7shared: §f" + (sharedOverride == null ? "-" : sharedOverride)
				+ " §8[" + source + "]");
	}

	private boolean localDefault(String module) {
		PluginConfiguration.ModuleSettings localModules = plugin.getLocalConfiguration().modules();
		return switch (module) {
			case "auth" -> localModules.auth();
			case "homes" -> localModules.homes();
			case "warps" -> localModules.warps();
			case "tpa" -> localModules.tpa();
			case "route-config" -> localModules.routeConfig();
			case "transfer-admin" -> localModules.transferAdmin();
			case "economy-bridge" -> localModules.economyBridge();
			case "permissions" -> localModules.permissions();
			default -> false;
		};
	}

	private SharedModuleConfigSnapshot mergeSharedSnapshot(SharedModuleConfigSnapshot current, String module, boolean value, boolean clear) {
		Boolean auth = current != null ? current.auth() : null;
		Boolean homes = current != null ? current.homes() : null;
		Boolean warps = current != null ? current.warps() : null;
		Boolean tpa = current != null ? current.tpa() : null;
		Boolean routeConfig = current != null ? current.routeConfig() : null;
		Boolean transferAdmin = current != null ? current.transferAdmin() : null;
		Boolean economyBridge = current != null ? current.economyBridge() : null;
		Boolean permissions = current != null ? current.permissions() : null;
		Boolean nextValue = clear ? null : value;
		switch (module) {
			case "auth" -> auth = nextValue;
			case "homes" -> homes = nextValue;
			case "warps" -> warps = nextValue;
			case "tpa" -> tpa = nextValue;
			case "route-config" -> routeConfig = nextValue;
			case "transfer-admin" -> transferAdmin = nextValue;
			case "economy-bridge" -> economyBridge = nextValue;
			case "permissions" -> permissions = nextValue;
			default -> throw new IllegalArgumentException("未知模块: " + module);
		}
		return new SharedModuleConfigSnapshot(1, auth, homes, warps, tpa, routeConfig, transferAdmin, economyBridge, permissions, null, null, null, null);
	}

	private String boolText(boolean value) {
		return value ? "§atrue" : "§cfalse";
	}

	private boolean canUseTransferCommands(CommandSender sender) {
		return transferAdminService != null && transferAdminMenuService != null && (
				sender.hasPermission(TRANSFER_VIEW_PERMISSION)
						|| sender.hasPermission(TRANSFER_MENU_PERMISSION)
						|| sender.hasPermission(TRANSFER_CLEAR_PERMISSION)
						|| sender.hasPermission(TRANSFER_RECONCILE_PERMISSION)
		);
	}

	private List<String> visibleTransferActions(CommandSender sender) {
		List<String> result = new ArrayList<>();
		result.add("help");
		if (sender.hasPermission(TRANSFER_VIEW_PERMISSION)) {
			result.add("info");
			result.add("history");
		}
		if (sender.hasPermission(TRANSFER_MENU_PERMISSION)) {
			result.add("menu");
			result.add("recent");
		}
		if (sender.hasPermission(TRANSFER_RECONCILE_PERMISSION)) {
			result.add("reconcile");
		}
		if (sender.hasPermission(TRANSFER_CLEAR_PERMISSION)) {
			result.add("clear");
		}
		return result;
	}

	private boolean handleAuthCommand(CommandSender sender, String label, String[] args) {
		if (!sender.hasPermission(AUTH_ADMIN_PERMISSION)) {
			sender.sendMessage("§c你没有权限执行 auth 管理命令。");
			return true;
		}
		if (!configuration.modules().auth()) {
			sender.sendMessage("§cAuth 模块当前已关闭。");
			return true;
		}
		if (args.length < 3) {
			sender.sendMessage("§e用法: /" + label + " auth <inspect|invalidate|forcereauth> <player>");
			return true;
		}
		String action = args[1];
		String playerName = args[2];
		try {
			Optional<UUID> playerId = storageProvider.findPlayerIdByName(playerName);
			if (playerId.isEmpty()) {
				sender.sendMessage("§c未找到玩家: " + playerName);
				return true;
			}
			if ("inspect".equalsIgnoreCase(action)) {
				var inspection = plugin.getApi().inspectAuth(playerId.get());
				sender.sendMessage("§aAuth 诊断");
				sender.sendMessage("§7玩家: §f" + playerName + " §8(" + playerId.get() + ")");
				sender.sendMessage("§7运行时状态: §f" + inspection.runtimeState());
				sender.sendMessage("§7失败次数: §f" + inspection.runtimeFailures());
				sender.sendMessage("§7资料存在: §f" + (inspection.profile() != null ? "是" : "否"));
				sender.sendMessage("§7票据存在: §f" + (inspection.ticket() != null ? "是" : "否"));
				return true;
			}
			if ("invalidate".equalsIgnoreCase(action)) {
				sender.sendMessage(plugin.getApi().invalidateAuthTickets(playerId.get(), sender.getName()));
				return true;
			}
			if ("forcereauth".equalsIgnoreCase(action)) {
				sender.sendMessage(plugin.getApi().forceReauthenticate(playerId.get(), sender.getName()));
				return true;
			}
		} catch (Exception exception) {
			sender.sendMessage("§c执行 auth 管理命令失败: " + exception.getMessage());
			return true;
		}
		sender.sendMessage("§e用法: /" + label + " auth <inspect|invalidate|forcereauth> <player>");
		return true;
	}

	private int parsePage(String input) {
		if (input == null || input.isBlank()) {
			return 1;
		}
		try {
			return Math.max(1, Integer.parseInt(input));
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private String formatStatus(String status) {
		return "online".equals(status) ? "§a在线" : "§c离线";
	}

	private String formatSince(Instant instant) {
		long seconds = Math.max(0L, Duration.between(instant, Instant.now()).getSeconds());
		if (seconds < 60) {
			return seconds + "秒前";
		}
		if (seconds < 3600) {
			return (seconds / 60) + "分钟前";
		}
		return (seconds / 3600) + "小时前";
	}
}
