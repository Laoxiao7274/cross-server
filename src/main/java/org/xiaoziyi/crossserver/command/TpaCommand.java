package org.xiaoziyi.crossserver.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.i18n.Texts;
import org.xiaoziyi.crossserver.player.PlayerLocationService;
import org.xiaoziyi.crossserver.player.PlayerLocationSnapshot;
import org.xiaoziyi.crossserver.teleport.TeleportInitiationResult;
import org.xiaoziyi.crossserver.teleport.TeleportRequestService;
import org.xiaoziyi.crossserver.teleport.TransferAdminService;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class TpaCommand implements org.bukkit.command.TabExecutor {
	private static final String PERMISSION = "crossserver.tpa.use";
	private static final String HERE_PERMISSION = "crossserver.tpa.here";

	private final JavaPlugin plugin;
	private final CrossServerApi api;
	private final TransferAdminService transferAdminService;
	private final TeleportRequestService requestService;
	private final PlayerLocationService playerLocationService;
	private final Texts texts;

	public TpaCommand(JavaPlugin plugin, CrossServerApi api, TransferAdminService transferAdminService, TeleportRequestService requestService, PlayerLocationService playerLocationService, Texts texts) {
		this.plugin = plugin;
		this.api = api;
		this.transferAdminService = transferAdminService;
		this.requestService = requestService;
		this.playerLocationService = playerLocationService;
		this.texts = texts;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(texts.tr("player.only"));
			return true;
		}
		String name = command.getName().toLowerCase(Locale.ROOT);
		switch (name) {
			case "tpa" -> handleTpa(sender, player, args);
			case "tpahere" -> handleTpaHere(sender, player, args);
			case "tpaccept" -> handleTpAccept(sender, player, args);
			case "tpdeny" -> handleTpDeny(sender, player, args);
			case "tpcancel" -> handleTpCancel(sender, player);
		}
		return true;
	}

	private void handleTpa(CommandSender sender, Player player, String[] args) {
		if (!sender.hasPermission(PERMISSION)) {
			sender.sendMessage(texts.tr("command.no_permission"));
			return;
		}
		if (args.length < 1) {
			sender.sendMessage(texts.tr("tpa.usage.tpa"));
			return;
		}
		ResolvedTarget target = resolveTarget(args[0]);
		if (target == null) {
			sender.sendMessage(texts.tr("tpa.target_not_found", args[0]));
			notifyFailure(player, "§c目标不可用", "§7未找到该在线玩家");
			return;
		}
		if (target.playerId().equals(player.getUniqueId())) {
			sender.sendMessage(texts.tr("tpa.self"));
			notifyFailure(player, "§c无法发送", "§7你不能请求自己");
			return;
		}
		if (!requestService.submitRequest(player.getUniqueId(), player.getName(), target.playerId(), target.playerName(), player.getServer().getName(), TeleportRequestService.TpaType.TPA)) {
			sender.sendMessage(texts.tr("tpa.request_save_failed"));
			notifyFailure(player, "§c发送失败", "§7传送请求未能保存");
			return;
		}
		notifySenderRequestSent(player, target.playerName(), false);
		Player onlineTarget = Bukkit.getPlayer(target.playerId());
		if (onlineTarget != null) {
			notifyReceiverRequest(onlineTarget, player.getName(), requestService.getExpirySeconds(), false);
		}
	}

	private void handleTpaHere(CommandSender sender, Player player, String[] args) {
		if (!sender.hasPermission(HERE_PERMISSION)) {
			sender.sendMessage(texts.tr("command.no_permission"));
			return;
		}
		if (args.length < 1) {
			sender.sendMessage(texts.tr("tpa.usage.tpahere"));
			return;
		}
		ResolvedTarget target = resolveTarget(args[0]);
		if (target == null) {
			sender.sendMessage(texts.tr("tpa.target_not_found", args[0]));
			notifyFailure(player, "§c目标不可用", "§7未找到该在线玩家");
			return;
		}
		if (target.playerId().equals(player.getUniqueId())) {
			sender.sendMessage(texts.tr("tpa.self"));
			notifyFailure(player, "§c无法发送", "§7你不能邀请自己");
			return;
		}
		if (!requestService.submitRequest(player.getUniqueId(), player.getName(), target.playerId(), target.playerName(), player.getServer().getName(), TeleportRequestService.TpaType.TPA_HERE)) {
			sender.sendMessage(texts.tr("tpa.request_save_failed"));
			notifyFailure(player, "§c发送失败", "§7传送邀请未能保存");
			return;
		}
		notifySenderRequestSent(player, target.playerName(), true);
		Player onlineTarget = Bukkit.getPlayer(target.playerId());
		if (onlineTarget != null) {
			notifyReceiverRequest(onlineTarget, player.getName(), requestService.getExpirySeconds(), true);
		}
	}

	private void handleTpAccept(CommandSender sender, Player player, String[] args) {
		if (!sender.hasPermission(PERMISSION)) {
			sender.sendMessage(texts.tr("command.no_permission"));
			return;
		}
		TeleportRequestService.PendingRequest consumed;
		if (args.length >= 1) {
			Optional<TransferAdminService.TransferInspection> inspection = inspect(args[0]);
			if (inspection.isEmpty()) {
				sender.sendMessage("§c未找到玩家: " + args[0]);
				return;
			}
			var pending = requestService.consumeRequest(player.getUniqueId(), inspection.get().playerId());
			if (pending.isEmpty()) {
				handleMissingSpecificRequest(player, args[0], inspection.get().playerId());
				return;
			}
			consumed = pending.get();
		} else {
			var latest = requestService.findLatestRequest(player.getUniqueId());
			if (latest.isEmpty()) {
				sender.sendMessage(texts.tr("tpa.no_pending"));
				notifyFailure(player, "§c没有请求", "§7当前没有待处理的 TPA");
				return;
			}
			var result = requestService.consumeRequest(player.getUniqueId(), latest.get().senderId());
			if (result.isEmpty()) {
				notifyRequestExpired(player, latest.get().senderName());
				Player requester = Bukkit.getPlayer(latest.get().senderId());
				if (requester != null) {
					notifyRequestExpired(requester, player.getName());
				}
				return;
			}
			consumed = result.get();
		}
		notifyAccepted(player, consumed.senderName(), false);
		Player senderPlayer = Bukkit.getPlayer(consumed.senderId());
		if (senderPlayer != null) {
			notifyAccepted(senderPlayer, player.getName(), true);
		}
		if (consumed.type() == TeleportRequestService.TpaType.TPA) {
			executeTeleportToReceiver(consumed, player);
		} else {
			executeTeleportReceiverToSender(consumed, player);
		}
	}

	private void executeTeleportToReceiver(TeleportRequestService.PendingRequest consumed, Player receiver) {
		Player senderPlayer = Bukkit.getPlayer(consumed.senderId());
		if (senderPlayer != null && senderPlayer.getServer().equals(receiver.getServer())) {
			Bukkit.getScheduler().runTask(plugin, () ->
				senderPlayer.teleportAsync(receiver.getLocation()).thenAccept(success -> {
					if (success) {
						notifyLocalTeleportSuccess(senderPlayer, receiver.getName());
						notifyWaitingCounterpart(receiver, senderPlayer.getName(), false, false);
					} else {
						notifyFailure(senderPlayer, "§c传送失败", "§7请稍后重试");
					}
				})
			);
			return;
		}
		TeleportInitiationResult result = api.requestTeleport(consumed.senderId(), playerLocationService.toTeleportTarget(new PlayerLocationSnapshot(
				receiver.getServer().getName(),
				receiver.getWorld().getName(),
				receiver.getLocation().getX(),
				receiver.getLocation().getY(),
				receiver.getLocation().getZ(),
				receiver.getLocation().getYaw(),
				receiver.getLocation().getPitch(),
				true,
				Instant.now()
		)), "tpa:" + receiver.getName());
		if (senderPlayer != null) {
			senderPlayer.sendMessage(result.message());
			if (!result.success()) {
				notifyFailure(senderPlayer, "§c跨服失败", "§7无法发起跨服传送");
			}
		}
		notifyWaitingCounterpart(receiver, consumed.senderName(), false, result.success());
	}

	private void executeTeleportReceiverToSender(TeleportRequestService.PendingRequest consumed, Player receiver) {
		Player senderPlayer = Bukkit.getPlayer(consumed.senderId());
		if (senderPlayer != null && senderPlayer.getServer().equals(receiver.getServer())) {
			Bukkit.getScheduler().runTask(plugin, () ->
				receiver.teleportAsync(senderPlayer.getLocation()).thenAccept(success -> {
					if (success) {
						notifyLocalTeleportSuccess(receiver, senderPlayer.getName());
						notifyWaitingCounterpart(senderPlayer, receiver.getName(), true, false);
					} else {
						notifyFailure(receiver, "§c传送失败", "§7请稍后重试");
					}
				})
			);
			return;
		}
		Optional<PlayerLocationSnapshot> senderLocation = playerLocationService.getPlayerLocation(consumed.senderId());
		if (senderLocation.isEmpty() || !senderLocation.get().online() || !playerLocationService.isFresh(senderLocation.get())) {
			receiver.sendMessage(texts.tr("tpa.requester_offline"));
			notifyFailure(receiver, "§c无法跨服", "§7请求发起者已不在线");
			return;
		}
		TeleportInitiationResult result = api.requestTeleport(receiver.getUniqueId(), playerLocationService.toTeleportTarget(senderLocation.get()), "tpahere:" + consumed.senderName());
		receiver.sendMessage(result.message());
		if (!result.success()) {
			notifyFailure(receiver, "§c跨服失败", "§7无法发起跨服传送");
		}
		if (senderPlayer != null) {
			notifyWaitingCounterpart(senderPlayer, receiver.getName(), true, result.success());
		}
	}

	private Optional<TransferAdminService.TransferInspection> inspect(String playerName) {
		try {
			return transferAdminService.inspectPlayer(playerName);
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	private ResolvedTarget resolveTarget(String playerName) {
		Player local = Bukkit.getPlayerExact(playerName);
		if (local != null) {
			return new ResolvedTarget(local.getUniqueId(), local.getName());
		}
		Optional<TransferAdminService.TransferInspection> inspection = inspect(playerName);
		if (inspection.isEmpty()) {
			return null;
		}
		Optional<PlayerLocationSnapshot> location = playerLocationService.getPlayerLocation(inspection.get().playerId());
		if (location.isEmpty() || !location.get().online() || !playerLocationService.isFresh(location.get())) {
			return null;
		}
		return new ResolvedTarget(inspection.get().playerId(), inspection.get().playerName());
	}

	private void handleTpDeny(CommandSender sender, Player player, String[] args) {
		if (!sender.hasPermission(PERMISSION)) {
			sender.sendMessage(texts.tr("command.no_permission"));
			return;
		}
		TeleportRequestService.PendingRequest consumed;
		if (args.length >= 1) {
			Optional<TransferAdminService.TransferInspection> inspection = inspect(args[0]);
			if (inspection.isEmpty()) {
				sender.sendMessage("§c未找到玩家: " + args[0]);
				return;
			}
			var pending = requestService.consumeRequest(player.getUniqueId(), inspection.get().playerId());
			if (pending.isEmpty()) {
				handleMissingSpecificRequest(player, args[0], inspection.get().playerId());
				return;
			}
			consumed = pending.get();
		} else {
			var latest = requestService.findLatestRequest(player.getUniqueId());
			if (latest.isEmpty()) {
				sender.sendMessage(texts.tr("tpa.no_pending"));
				notifyFailure(player, "§c没有请求", "§7当前没有待处理的 TPA");
				return;
			}
			var result = requestService.consumeRequest(player.getUniqueId(), latest.get().senderId());
			if (result.isEmpty()) {
				notifyRequestExpired(player, latest.get().senderName());
				Player requester = Bukkit.getPlayer(latest.get().senderId());
				if (requester != null) {
					notifyRequestExpired(requester, player.getName());
				}
				return;
			}
			consumed = result.get();
		}
		notifyDenied(player, consumed.senderName(), false);
		Player requester = Bukkit.getPlayer(consumed.senderId());
		if (requester != null) {
			notifyDenied(requester, player.getName(), true);
		}
	}

	private void handleTpCancel(CommandSender sender, Player player) {
		if (!sender.hasPermission(PERMISSION)) {
			sender.sendMessage(texts.tr("command.no_permission"));
			return;
		}
		List<TeleportRequestService.PendingRequest> removed = requestService.removeRequestsBySender(player.getUniqueId());
		if (removed.isEmpty()) {
			player.sendMessage(texts.tr("tpa.no_pending_cancel"));
			player.sendActionBar(Component.text(texts.tr("tpa.no_pending_actionbar")));
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.9F);
			return;
		}
		notifyCancelled(player, null, removed.size(), false);
		for (TeleportRequestService.PendingRequest request : removed) {
			Player receiver = Bukkit.getPlayer(request.receiverId());
			if (receiver != null) {
				notifyCancelled(receiver, player.getName(), 1, true);
			}
		}
	}

	private void handleMissingSpecificRequest(Player player, String playerName, UUID senderId) {
		TeleportRequestService.RequestStatus status = requestService.getRequestStatus(player.getUniqueId(), senderId);
		if (status == TeleportRequestService.RequestStatus.EXPIRED) {
			notifyRequestExpired(player, playerName);
			return;
		}
		player.sendMessage(texts.tr("tpa.missing_specific", playerName));
		notifyFailure(player, "§c没有请求", "§7指定玩家没有待处理请求");
	}

	private void notifySenderRequestSent(Player player, String targetName, boolean here) {
		String chat = here
				? "§a已邀请 " + targetName + " 传送到你这里。§7(" + requestService.getExpirySeconds() + "秒有效)"
				: "§a已向 " + targetName + " 发送传送请求。§7(" + requestService.getExpirySeconds() + "秒有效)";
		String title = here ? "§a传送邀请已发送" : "§a传送请求已发送";
		player.sendMessage(chat);
		player.sendTitle(title, "§7目标: §f" + targetName, 5, 40, 10);
		player.sendActionBar(Component.text("请求有效期 " + requestService.getExpirySeconds() + " 秒"));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7F, 1.2F);
	}

	private void notifyReceiverRequest(Player player, String senderName, int expirySeconds, boolean here) {
		String firstLine = here
				? "§e" + senderName + " 邀请你传送到他那里。§7(" + expirySeconds + "秒有效)"
				: "§e" + senderName + " 请求传送到你这里。§7(" + expirySeconds + "秒有效)";
		String title = here ? "§6收到 tpahere 邀请" : "§6收到 TPA 请求";
		player.sendMessage(firstLine);
		player.sendMessage("§7输入 §a/tpaccept " + senderName + " §7接受，或 §c/tpdeny " + senderName + " §7拒绝。");
		player.sendTitle(title, "§f来自 " + senderName, 5, 50, 10);
		player.sendActionBar(Component.text("/tpaccept " + senderName + " 或 /tpdeny " + senderName));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9F, 1.15F);
	}

	private void notifyAccepted(Player player, String otherName, boolean requesterSide) {
		String message = requesterSide ? "§a" + otherName + " 已接受你的请求。" : "§a已接受 " + otherName + " 的请求。";
		player.sendMessage(message);
		player.sendTitle("§a请求已接受", "§f" + otherName, 5, 35, 10);
		player.sendActionBar(Component.text("传送即将开始"));
		player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.2F);
	}

	private void notifyDenied(Player player, String otherName, boolean requesterSide) {
		String message = requesterSide ? "§c" + otherName + " 拒绝了你的请求。" : "§e已拒绝 " + otherName + " 的请求。";
		player.sendMessage(message);
		player.sendTitle("§c请求被拒绝", "§f" + otherName, 5, 35, 10);
		player.sendActionBar(Component.text("TPA 请求已结束"));
		player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
	}

	private void notifyCancelled(Player player, String actorName, int count, boolean receiverSide) {
		String message = receiverSide
				? "§e" + actorName + " 已取消之前发给你的传送请求。"
				: "§a已取消 " + count + " 个待处理的传送请求。";
		String subtitle = receiverSide ? "§7来自 §f" + actorName : "§7待处理请求已清空";
		player.sendMessage(message);
		player.sendTitle("§e请求已取消", subtitle, 5, 35, 10);
		player.sendActionBar(Component.text(receiverSide ? "对方已取消 TPA 请求" : "已取消所有待处理请求"));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.95F);
	}

	private void notifyRequestExpired(Player player, String otherName) {
		player.sendMessage("§e来自/发往 " + otherName + " 的 TPA 请求已过期。");
		player.sendTitle("§e请求已过期", "§f" + otherName, 5, 35, 10);
		player.sendActionBar(Component.text("请重新发送请求"));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.9F);
	}

	private void notifyWaitingCounterpart(Player player, String otherName, boolean tpahere, boolean crossServerSuccess) {
		String message;
		if (!crossServerSuccess) {
			message = "§c跨服传送发起失败。";
		} else if (tpahere) {
			message = "§a对方正在跨服传送到你这里。";
		} else {
			message = "§a正在等待对方跨服传送过来。";
		}
		player.sendMessage(message);
		player.sendActionBar(Component.text(message.replace("§a", "").replace("§c", "").replace("§e", "")));
		player.playSound(player.getLocation(), crossServerSuccess ? Sound.BLOCK_BEACON_AMBIENT : Sound.ENTITY_VILLAGER_NO, 0.7F, crossServerSuccess ? 1.2F : 1.0F);
	}

	private void notifyLocalTeleportSuccess(Player player, String otherName) {
		player.sendMessage("§a已传送到 " + otherName + "。");
		player.sendTitle("§a传送成功", "§f" + otherName, 5, 30, 10);
		player.sendActionBar(Component.text("本服传送完成"));
		player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.15F);
	}

	private void notifyFailure(Player player, String title, String subtitle) {
		player.sendTitle(title, subtitle, 5, 35, 10);
		player.sendActionBar(Component.text(subtitle.replace("§7", "").replace("§c", "")));
		player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8F, 1.0F);
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
		if (args.length == 1) {
			String name = command.getName().toLowerCase(Locale.ROOT);
			if ("tpa".equals(name) || "tpahere".equals(name)) {
				return null;
			}
			if ("tpaccept".equals(name) || "tpdeny".equals(name)) {
				if (sender instanceof Player player) {
					return player.getServer().getOnlinePlayers().stream()
							.filter(p -> !p.equals(player))
							.map(Player::getName)
							.toList();
				}
			}
		}
		return List.of();
	}

	private record ResolvedTarget(UUID playerId, String playerName) {
	}
}
