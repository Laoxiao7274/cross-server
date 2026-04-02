package org.xiaoziyi.crossserver.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigLoader {
	public PluginConfiguration load(JavaPlugin plugin) {
		plugin.saveDefaultConfig();
		plugin.reloadConfig();
		FileConfiguration config = plugin.getConfig();

		String serverId = requireText(config.getString("server.id", "").trim(), "server.id");
		String cluster = requireText(config.getString("server.cluster", "default").trim(), "server.cluster");
		String jdbcUrl = requireText(config.getString("database.jdbc-url", "").trim(), "database.jdbc-url");
		String username = requireText(config.getString("database.username", "").trim(), "database.username");
		String password = config.getString("database.password", "");
		boolean messagingEnabled = config.getBoolean("messaging.enabled", true);
		String redisUri = messagingEnabled
				? requireText(config.getString("messaging.redis-uri", "redis://127.0.0.1:6379/0").trim(), "messaging.redis-uri")
				: config.getString("messaging.redis-uri", "redis://127.0.0.1:6379/0").trim();
		String channel = messagingEnabled
				? requireText(config.getString("messaging.channel", "cross-server:sync").trim(), "messaging.channel")
				: config.getString("messaging.channel", "cross-server:sync").trim();
		String kickMessage = requireText(config.getString("session.kick-message", "你的跨服会话正在同步中，请稍后重试").trim(), "session.kick-message");

		return new PluginConfiguration(
				new PluginConfiguration.ServerSettings(serverId, cluster),
				new PluginConfiguration.DatabaseSettings(
						jdbcUrl,
						username,
						password,
						Math.max(2, config.getInt("database.maximum-pool-size", 10))
				),
				new PluginConfiguration.MessagingSettings(
						messagingEnabled,
						redisUri,
						channel
				),
				new PluginConfiguration.SessionSettings(
						Math.max(5, config.getInt("session.lock-seconds", 30)),
						Math.max(3, config.getInt("session.heartbeat-seconds", 10)),
						kickMessage
				),
				new PluginConfiguration.NodeSettings(
						Math.max(5, config.getInt("node.heartbeat-seconds", 15)),
						Math.max(15, config.getInt("node.offline-seconds", 45))
				),
				new PluginConfiguration.TeleportSettings(
						Math.max(10, config.getInt("teleport.handoff-seconds", 30)),
						Math.max(1, config.getInt("teleport.arrival-check-delay-ticks", 10)),
						Math.max(0, config.getInt("teleport.cooldown-seconds", 10)),
						new PluginConfiguration.TeleportSettings.GatewaySettings(
								config.getString("teleport.gateway.type", "unsupported").trim(),
								requireText(config.getString("teleport.gateway.plugin-message-channel", "BungeeCord").trim(), "teleport.gateway.plugin-message-channel"),
								requireText(config.getString("teleport.gateway.connect-subchannel", "Connect").trim(), "teleport.gateway.connect-subchannel"),
								readServerMap(config.getConfigurationSection("teleport.gateway.server-map"))
						)
				),
				new PluginConfiguration.ModuleSettings(
						config.getBoolean("modules.auth", true),
						config.getBoolean("modules.homes", true),
						config.getBoolean("modules.warps", true),
						config.getBoolean("modules.tpa", true),
						config.getBoolean("modules.route-config", true),
						config.getBoolean("modules.transfer-admin", true),
						config.getBoolean("modules.economy-bridge", true),
						config.getBoolean("modules.permissions", false)
				)
		);
	}

	private Map<String, String> readServerMap(ConfigurationSection section) {
		if (section == null) {
			return Map.of();
		}
		Map<String, String> serverMap = new LinkedHashMap<>();
		for (String key : section.getKeys(false)) {
			String value = requireText(section.getString(key, "").trim(), "teleport.gateway.server-map." + key);
			serverMap.put(key, value);
		}
		return Map.copyOf(serverMap);
	}

	private String requireText(String value, String path) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("配置项不能为空: " + path);
		}
		return value;
	}
}
