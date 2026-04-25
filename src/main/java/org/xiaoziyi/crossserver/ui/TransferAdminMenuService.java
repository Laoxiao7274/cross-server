package org.xiaoziyi.crossserver.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.session.SessionTransferState;
import org.xiaoziyi.crossserver.teleport.TeleportHandoff;
import org.xiaoziyi.crossserver.teleport.TeleportHandoffStatus;
import org.xiaoziyi.crossserver.teleport.TransferAdminService;
import org.xiaoziyi.crossserver.teleport.TransferDiagnostics;
import org.xiaoziyi.crossserver.teleport.TransferHistoryEntry;
import org.xiaoziyi.crossserver.teleport.TransferPageResult;
import org.xiaoziyi.crossserver.teleport.TransferRecoveryStatus;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TransferAdminMenuService {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final int PAGE_SIZE = 21;
	private final JavaPlugin plugin;
	private final TransferAdminService transferAdminService;

	public TransferAdminMenuService(JavaPlugin plugin, TransferAdminService transferAdminService) {
		this.plugin = plugin;
		this.transferAdminService = transferAdminService;
	}

	public void openMenu(Player viewer, TransferAdminService.TransferInspection inspection) {
		openDetailMenu(viewer, inspection, 1);
	}

	public void openDetailMenu(Player viewer, TransferAdminService.TransferInspection inspection, int parentPage) {
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			viewer.openInventory(createDetailInventory(inspection, parentPage));
			viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.1F);
		});
	}

	public void openDetailMenuAsync(Player viewer, UUID targetPlayerId, int parentPage) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				TransferAdminService.TransferInspection inspection = transferAdminService.inspectPlayer(targetPlayerId);
				TransferAdminService.TransferInspection captured = inspection;
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					openDetailMenu(viewer, captured, parentPage);
				});
			} catch (Exception exception) {
				plugin.getServer().getScheduler().runTask(plugin, () -> viewer.sendMessage("§c打开 transfer 菜单失败: " + exception.getMessage()));
			}
		});
	}

	public void openRecentMenu(Player viewer) {
		openRecentMenu(viewer, 1);
	}

	public void openRecentMenu(Player viewer, int page) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				TransferPageResult pageResult = transferAdminService.getRecentTransferHistoryPage(page, PAGE_SIZE);
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					viewer.openInventory(createRecentInventory(pageResult));
					viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.1F);
				});
			} catch (Exception exception) {
				plugin.getServer().getScheduler().runTask(plugin, () -> viewer.sendMessage("§c打开 transfer recent 菜单失败: " + exception.getMessage()));
			}
		});
	}

	public void openHistoryMenu(Player viewer, UUID targetPlayerId, int page, int parentPage) {
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				TransferAdminService.TransferInspection inspection = transferAdminService.inspectPlayer(targetPlayerId);
				TransferPageResult pageResult = transferAdminService.getTransferHistoryPage(targetPlayerId, page, PAGE_SIZE);
				plugin.getServer().getScheduler().runTask(plugin, () -> {
					viewer.openInventory(createHistoryInventory(inspection, pageResult, parentPage));
					viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6F, 1.1F);
				});
			} catch (Exception exception) {
				plugin.getServer().getScheduler().runTask(plugin, () -> viewer.sendMessage("§c打开 transfer history 菜单失败: " + exception.getMessage()));
			}
		});
	}

	public void handleClick(Player player, TransferAdminMenuHolder holder, int slot) {
		if (holder.recentView()) {
			handleRecentClick(player, holder, slot);
			return;
		}
		if (holder.historyView()) {
			handleHistoryClick(player, holder, slot);
			return;
		}
		handleDetailClick(player, holder, slot);
	}

	public void sendInspection(CommandSender sender, TransferAdminService.TransferInspection inspection) {
		TeleportHandoff handoff = inspection.handoff();
		sender.sendMessage("§aTransfer 诊断");
		sender.sendMessage("§7玩家: §f" + inspection.playerName() + " §8(" + inspection.playerId() + ")");
		sender.sendMessage("§7本地 Prepared transfer: §f" + (inspection.localPreparedTransfer() ? "是" : "否"));
		SessionTransferState sessionTransferState = inspection.sessionTransferState();
		if (sessionTransferState != null) {
			sender.sendMessage("§7Session server: §f" + valueOrDash(sessionTransferState.currentServer()));
			sender.sendMessage("§7Session transfer token: §f" + (sessionTransferState.hasPreparedTransfer() ? "存在" : "无"));
			sender.sendMessage("§7Session transfer target: §f" + valueOrDash(sessionTransferState.transferTargetServer()));
			sender.sendMessage("§7Session transfer expires: §f" + formatTime(sessionTransferState.transferExpiresAt()));
		}
		sender.sendMessage("§7Recovery status: §f" + inspection.recoveryStatus());
		for (String action : inspection.suggestedActions()) {
			sender.sendMessage("§7建议: §f" + action);
		}
		if (handoff == null) {
			sender.sendMessage("§7该玩家当前没有跨服传送 handoff 记录。§8(local prepared=" + inspection.localPreparedTransfer() + ")");
			return;
		}
		sender.sendMessage("§7请求: §f" + handoff.requestId());
		sender.sendMessage("§7状态: " + colorStatus(handoff.status()) + handoff.status());
		sender.sendMessage("§7来源/目标: §f" + handoff.sourceServerId() + " §7-> §f" + handoff.targetServerId());
		sender.sendMessage("§7原因: §f" + handoff.cause() + " §8/ §f" + handoff.causeRef());
		sender.sendMessage("§7创建时间: §f" + formatTime(handoff.createdAt()));
		sender.sendMessage("§7过期时间: §f" + formatTime(handoff.expiresAt()));
		sender.sendMessage("§7Gateway 发送: §f" + formatTime(handoff.gatewaySentAt()));
		sender.sendMessage("§7落点应用: §f" + formatTime(handoff.consumedAt()));
		sender.sendMessage("§7ACK 时间: §f" + formatTime(handoff.ackedAt()));
		sender.sendMessage("§7ACK 节点: §f" + valueOrDash(handoff.ackedByServerId()));
		sender.sendMessage("§7Recovery state: §f" + valueOrDash(handoff.recoveryState()));
		sender.sendMessage("§7Prepared transfer: §f" + handoff.preparedTransferState());
		sender.sendMessage("§7Prepared 清理时间: §f" + formatTime(handoff.preparedTransferClearedAt()));
		sender.sendMessage("§7最后更新时间: §f" + formatTime(handoff.lastUpdatedAt()));
		sender.sendMessage("§7失败原因: §f" + valueOrDash(handoff.failureReason()));
	}

	private void handleRecentClick(Player player, TransferAdminMenuHolder holder, int slot) {
		if (slot == 45 && holder.page() > 1) {
			openRecentMenu(player, holder.page() - 1);
			return;
		}
		if (slot == 49) {
			openRecentMenu(player, holder.page());
			return;
		}
		if (slot == 53) {
			openRecentMenu(player, holder.page() + 1);
			return;
		}
		if (slot == 22) {
			player.closeInventory();
			return;
		}
		UUID target = holder.targetAt(slot);
		if (target != null) {
			openDetailMenuAsync(player, target, holder.page());
		}
	}

	private void handleHistoryClick(Player player, TransferAdminMenuHolder holder, int slot) {
		if (slot == 45 && holder.page() > 1) {
			openHistoryMenu(player, holder.targetPlayerId(), holder.page() - 1, holder.parentPage());
			return;
		}
		if (slot == 49) {
			openDetailMenuAsync(player, holder.targetPlayerId(), holder.parentPage());
			return;
		}
		if (slot == 53) {
			openHistoryMenu(player, holder.targetPlayerId(), holder.page() + 1, holder.parentPage());
			return;
		}
		if (slot == 22) {
			player.closeInventory();
		}
	}

	private void handleDetailClick(Player player, TransferAdminMenuHolder holder, int slot) {
		if (slot == 11) {
			openDetailMenuAsync(player, holder.targetPlayerId(), holder.parentPage());
			return;
		}
		if (slot == 15) {
			if (!player.hasPermission("crossserver.transfer.clear")) {
				player.sendMessage("§c你没有权限清理 transfer 状态。");
				player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7F, 1.0F);
				return;
			}
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
				try {
					TransferAdminService.ClearResult result = transferAdminService.clearTransfer(holder.targetPlayerId(), player.getName());
					plugin.getServer().getScheduler().runTask(plugin, () -> {
						player.sendMessage(result.message());
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7F, 1.2F);
						openDetailMenuAsync(player, holder.targetPlayerId(), holder.parentPage());
					});
				} catch (Exception exception) {
					plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage("§c清理 transfer 状态失败: " + exception.getMessage()));
				}
			});
			return;
		}
		if (slot == 31) {
			openHistoryMenu(player, holder.targetPlayerId(), 1, holder.parentPage());
			return;
		}
		if (slot == 40) {
			openRecentMenu(player, holder.parentPage());
			return;
		}
		if (slot == 22) {
			player.closeInventory();
		}
	}

	private Inventory createDetailInventory(TransferAdminService.TransferInspection inspection, int parentPage) {
		TransferAdminMenuHolder holder = new TransferAdminMenuHolder(inspection.playerId(), TransferAdminMenuHolder.ViewMode.DETAIL, 1, parentPage);
		Inventory inventory = Bukkit.createInventory(holder, 45, "✦ Transfer 管理 ✦ " + inspection.playerName());
		holder.bind(inventory);
		fillBackground(inventory, List.of(10, 11, 12, 13, 14, 15, 16, 22, 31, 40));
		inventory.setItem(10, item(Material.PLAYER_HEAD, "§a玩家", List.of(
				"§7名称: §f" + inspection.playerName(),
				"§7UUID: §f" + inspection.playerId(),
				"§7Recovery: §f" + inspection.recoveryStatus(),
				"§7本地 Prepared: §f" + (inspection.localPreparedTransfer() ? "是" : "否")
		)));
		inventory.setItem(11, item(Material.COMPASS, "§b刷新诊断", List.of("§7重新加载 handoff / history / baton 状态")));
		inventory.setItem(15, item(Material.BARRIER, "§c清理 transfer", List.of("§7清理 handoff 与 prepared transfer", "§8需要 crossserver.transfer.clear")));
		inventory.setItem(22, item(Material.OAK_DOOR, "§7关闭", List.of("§7关闭菜单")));
		inventory.setItem(31, item(Material.WRITABLE_BOOK, "§d查看历史", historySummaryLore(inspection.recentHistory())));
		inventory.setItem(40, item(Material.SPECTRAL_ARROW, "§e返回 recent", List.of("§7返回 recent 第 §f" + parentPage + " §7页")));
		TeleportHandoff handoff = inspection.handoff();
		SessionTransferState sessionTransferState = inspection.sessionTransferState();
		if (handoff == null) {
			inventory.setItem(12, item(Material.PAPER, "§e无 handoff", List.of("§7当前没有 teleport.handoff 快照记录")));
		} else {
			inventory.setItem(12, item(statusMaterial(handoff.status()), colorStatus(handoff.status()) + "状态", List.of(
					"§7请求: §f" + handoff.requestId(),
					"§7状态: " + colorStatus(handoff.status()) + handoff.status(),
					"§7Prepared: §f" + handoff.preparedTransferState(),
					"§7Recovery: §f" + valueOrDash(handoff.recoveryState()),
					"§7失败原因: §f" + valueOrDash(handoff.failureReason())
			)));
			inventory.setItem(13, item(Material.MAP, "§b路由", List.of(
					"§7来源: §f" + handoff.sourceServerId(),
					"§7目标: §f" + handoff.targetServerId(),
					"§7世界: §f" + handoff.targetWorld(),
					"§7原因: §f" + handoff.cause() + " / " + handoff.causeRef()
			)));
			inventory.setItem(14, item(Material.CLOCK, "§e时间线", List.of(
					"§7创建: §f" + formatTime(handoff.createdAt()),
					"§7过期: §f" + formatTime(handoff.expiresAt()),
					"§7Gateway: §f" + formatTime(handoff.gatewaySentAt()),
					"§7到达: §f" + formatTime(handoff.consumedAt()),
					"§7ACK: §f" + formatTime(handoff.ackedAt()),
					"§7更新: §f" + formatTime(handoff.lastUpdatedAt())
			)));
		}
		inventory.setItem(16, item(Material.REDSTONE, "§cBaton / Prepared", batonLore(sessionTransferState, inspection.localPreparedTransfer(), inspection.suggestedActions())));
		return inventory;
	}

	private Inventory createRecentInventory(TransferPageResult pageResult) {
		TransferAdminMenuHolder holder = new TransferAdminMenuHolder(new UUID(0L, 0L), TransferAdminMenuHolder.ViewMode.RECENT, pageResult.page(), pageResult.page());
		Inventory inventory = Bukkit.createInventory(holder, 54, "✦ Transfer Recent ✦ " + pageResult.page() + "/" + pageResult.totalPages());
		holder.bind(inventory);
		fillBackground(inventory, List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 45, 47, 49, 51, 53));
		int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
		List<TransferHistoryEntry> recent = pageResult.entries();
		for (int index = 0; index < recent.size() && index < slots.length; index++) {
			TransferHistoryEntry entry = recent.get(index);
			int slot = slots[index];
			holder.bindTarget(slot, entry.playerId());
			inventory.setItem(slot, item(statusMaterial(entry.status() == null ? TeleportHandoffStatus.CANCELLED : entry.status()), "§b" + valueOrDash(entry.playerName()), List.of(
					"§7请求: §f" + entry.requestId(),
					"§7事件: §f" + entry.eventType(),
					"§7状态: §f" + (entry.status() == null ? "-" : entry.status()),
					"§7路由: §f" + valueOrDash(entry.sourceServerId()) + " §7-> §f" + valueOrDash(entry.targetServerId()),
					"§7时间: §f" + formatTime(entry.createdAt()),
					"§7详情: §f" + valueOrDash(entry.detail()),
					"",
					"§a点击查看该玩家详情"
			)));
		}
		inventory.setItem(45, item(pageResult.page() > 1 ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e上一页", pageResult.page() > 1 ? List.of("§7查看上一页") : List.of("§8已经是第一页")));
		inventory.setItem(47, item(Material.BOOK, "§b统计", List.of(
				"§7记录数: §f" + pageResult.totalItems(),
				"§7页码: §f" + pageResult.page() + "/" + pageResult.totalPages(),
				"§7每页: §f" + pageResult.pageSize()
		)));
		inventory.setItem(49, item(Material.COMPASS, "§b刷新", List.of("§7重新加载 recent transfer")));
		inventory.setItem(51, item(Material.CLOCK, "§d说明", List.of(
				"§7展示最近 transfer 事件",
				"§7点击任意记录可查看玩家详情",
				"§8详情页可继续查看历史与清理"
		)));
		inventory.setItem(53, item(pageResult.page() < pageResult.totalPages() ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e下一页", pageResult.page() < pageResult.totalPages() ? List.of("§7查看下一页") : List.of("§8已经是最后一页")));
		return inventory;
	}

	private Inventory createHistoryInventory(TransferAdminService.TransferInspection inspection, TransferPageResult pageResult, int parentPage) {
		TransferAdminMenuHolder holder = new TransferAdminMenuHolder(inspection.playerId(), TransferAdminMenuHolder.ViewMode.HISTORY, pageResult.page(), parentPage);
		Inventory inventory = Bukkit.createInventory(holder, 54, "✦ Transfer History ✦ " + inspection.playerName() + " " + pageResult.page() + "/" + pageResult.totalPages());
		holder.bind(inventory);
		fillBackground(inventory, List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 45, 47, 49, 51, 53));
		int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
		for (int index = 0; index < pageResult.entries().size() && index < slots.length; index++) {
			TransferHistoryEntry entry = pageResult.entries().get(index);
			inventory.setItem(slots[index], item(statusMaterial(entry.status() == null ? TeleportHandoffStatus.CANCELLED : entry.status()), "§b" + entry.eventType(), List.of(
					"§7请求: §f" + entry.requestId(),
					"§7状态: §f" + valueOrDash(entry.status() == null ? null : entry.status().name()),
					"§7时间: §f" + formatTime(entry.createdAt()),
					"§7来源: §f" + valueOrDash(entry.sourceServerId()),
					"§7目标: §f" + valueOrDash(entry.targetServerId()),
					"§7详情: §f" + valueOrDash(entry.detail())
			)));
		}
		inventory.setItem(45, item(pageResult.page() > 1 ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e上一页", pageResult.page() > 1 ? List.of("§7查看上一页") : List.of("§8已经是第一页")));
		inventory.setItem(47, item(Material.BOOK, "§b统计", List.of(
				"§7历史记录: §f" + pageResult.totalItems(),
				"§7页码: §f" + pageResult.page() + "/" + pageResult.totalPages(),
				"§7玩家: §f" + inspection.playerName()
		)));
		inventory.setItem(49, item(Material.SPECTRAL_ARROW, "§e返回详情", List.of("§7返回玩家 transfer 详情")));
		inventory.setItem(51, item(Material.CLOCK, "§d状态", List.of(
				"§7Recovery: §f" + inspection.recoveryStatus(),
				"§7本地 Prepared: §f" + (inspection.localPreparedTransfer() ? "是" : "否")
		)));
		inventory.setItem(53, item(pageResult.page() < pageResult.totalPages() ? Material.SPECTRAL_ARROW : Material.BARRIER, "§e下一页", pageResult.page() < pageResult.totalPages() ? List.of("§7查看下一页") : List.of("§8已经是最后一页")));
		return inventory;
	}

	private List<String> batonLore(SessionTransferState sessionTransferState, boolean localPreparedTransfer, List<String> suggestedActions) {
		List<String> lore = new ArrayList<>();
		lore.add("§7本地 Prepared: §f" + (localPreparedTransfer ? "是" : "否"));
		if (sessionTransferState == null) {
			lore.add("§7Session baton: §f-");
		} else {
			lore.add("§7Session server: §f" + valueOrDash(sessionTransferState.currentServer()));
			lore.add("§7Transfer token: §f" + (sessionTransferState.hasPreparedTransfer() ? "存在" : "无"));
			lore.add("§7Transfer target: §f" + valueOrDash(sessionTransferState.transferTargetServer()));
			lore.add("§7Transfer expires: §f" + formatTime(sessionTransferState.transferExpiresAt()));
		}
		if (!suggestedActions.isEmpty()) {
			lore.add("");
			for (String action : suggestedActions.subList(0, Math.min(3, suggestedActions.size()))) {
				lore.add("§e" + action);
			}
		}
		return lore;
	}

	private List<String> historySummaryLore(List<TransferHistoryEntry> history) {
		if (history.isEmpty()) {
			return List.of("§7暂无 transfer 历史", "", "§a点击查看完整历史页");
		}
		List<String> lore = new ArrayList<>();
		for (TransferHistoryEntry entry : history.subList(0, Math.min(3, history.size()))) {
			lore.add("§7- §f" + entry.eventType() + " §8(" + formatTime(entry.createdAt()) + ")");
		}
		lore.add("");
		lore.add("§a点击查看完整历史页");
		return lore;
	}

	private void fillBackground(Inventory inventory, List<Integer> contentSlots) {
		for (int slot = 0; slot < inventory.getSize(); slot++) {
			inventory.setItem(slot, item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
		}
		for (int slot : contentSlots) {
			inventory.setItem(slot, null);
		}
	}

	private ItemStack item(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	private Material statusMaterial(TeleportHandoffStatus status) {
		return switch (status) {
			case PREPARING -> Material.YELLOW_STAINED_GLASS;
			case PENDING -> Material.ENDER_EYE;
			case CONSUMED -> Material.LIME_STAINED_GLASS;
			case FAILED -> Material.RED_STAINED_GLASS;
			case EXPIRED -> Material.ORANGE_STAINED_GLASS;
			case CANCELLED -> Material.GRAY_STAINED_GLASS;
		};
	}

	private String colorStatus(TeleportHandoffStatus status) {
		return switch (status) {
			case PREPARING -> "§e";
			case PENDING -> "§b";
			case CONSUMED -> "§a";
			case FAILED -> "§c";
			case EXPIRED -> "§6";
			case CANCELLED -> "§7";
		};
	}

	private String formatTime(java.time.Instant instant) {
		return instant == null ? "-" : TIME_FORMATTER.format(instant);
	}

	private String valueOrDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
