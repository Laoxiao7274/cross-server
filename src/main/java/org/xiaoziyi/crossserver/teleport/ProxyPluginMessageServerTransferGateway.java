package org.xiaoziyi.crossserver.teleport;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.config.PluginConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public final class ProxyPluginMessageServerTransferGateway implements ServerTransferGateway {
	private final JavaPlugin plugin;
	private final Logger logger;
	private final String channel;
	private final String connectSubchannel;
	private final Map<String, String> serverMap;

	public ProxyPluginMessageServerTransferGateway(JavaPlugin plugin, Logger logger, PluginConfiguration.TeleportSettings.GatewaySettings settings) {
		this.plugin = plugin;
		this.logger = logger;
		this.channel = settings.pluginMessageChannel();
		this.connectSubchannel = settings.connectSubchannel();
		this.serverMap = settings.serverMap();
	}

	@Override
	public TeleportInitiationResult transfer(Player player, TeleportHandoff handoff) {
		if (!player.isOnline()) {
			return new TeleportInitiationResult(false, true, "§c玩家已离线，无法发起代理切服。", handoff.requestId());
		}
		String proxyServerName = serverMap.get(handoff.targetServerId());
		if (proxyServerName == null || proxyServerName.isBlank()) {
			return new TeleportInitiationResult(false, true, "§c目标服务器未配置代理路由映射。", handoff.requestId());
		}
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(byteStream)) {
				output.writeUTF(connectSubchannel);
				output.writeUTF(proxyServerName);
			}
			player.sendPluginMessage(plugin, channel, byteStream.toByteArray());
			logger.info("已发送代理切服请求: requestId=" + handoff.requestId() + " player=" + player.getUniqueId() + " targetServerId=" + handoff.targetServerId() + " proxyTarget=" + proxyServerName);
			return new TeleportInitiationResult(true, true, successMessage(handoff), handoff.requestId());
		} catch (IOException exception) {
			logger.warning("发送代理切服消息失败: requestId=" + handoff.requestId() + " -> " + exception.getMessage());
			return new TeleportInitiationResult(false, true, "§c代理切服消息发送失败，请稍后重试。", handoff.requestId());
		}
	}

	private String successMessage(TeleportHandoff handoff) {
		return switch (handoff.cause()) {
			case HOME -> "§a正在前往家园，正在连接目标服务器...";
			case WARP -> "§a正在前往地标，正在连接目标服务器...";
			case TPA -> "§a正在前往目标玩家，正在连接目标服务器...";
			case TPA_HERE -> "§a正在处理玩家传送邀请，正在连接目标服务器...";
		};
	}

	public String channel() {
		return channel;
	}
}
