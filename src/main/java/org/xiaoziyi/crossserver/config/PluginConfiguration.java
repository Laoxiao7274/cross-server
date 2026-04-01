package org.xiaoziyi.crossserver.config;

import java.util.Map;

public record PluginConfiguration(
		ServerSettings server,
		DatabaseSettings database,
		MessagingSettings messaging,
		SessionSettings session,
		NodeSettings node,
		TeleportSettings teleport
) {
	public PluginConfiguration withTeleportGatewayServerMap(Map<String, String> serverMap) {
		return new PluginConfiguration(
				server,
				database,
				messaging,
				session,
				node,
				new TeleportSettings(
						teleport.handoffSeconds(),
						teleport.arrivalCheckDelayTicks(),
						teleport.cooldownSeconds(),
						new TeleportSettings.GatewaySettings(
								teleport.gateway().type(),
								teleport.gateway().pluginMessageChannel(),
								teleport.gateway().connectSubchannel(),
								Map.copyOf(serverMap)
						)
				)
		);
	}
	public record ServerSettings(
			String id,
			String cluster
	) {
	}

	public record DatabaseSettings(
			String jdbcUrl,
			String username,
			String password,
			int maximumPoolSize
	) {
	}

	public record MessagingSettings(
			boolean enabled,
			String redisUri,
			String channel
	) {
	}

	public record SessionSettings(
			int lockSeconds,
			int heartbeatSeconds,
			String kickMessage
	) {
	}

	public record NodeSettings(
			int heartbeatSeconds,
			int offlineSeconds
	) {
	}

	public record TeleportSettings(
			int handoffSeconds,
			int arrivalCheckDelayTicks,
			int cooldownSeconds,
			GatewaySettings gateway
		) {
		public record GatewaySettings(
				String type,
				String pluginMessageChannel,
				String connectSubchannel,
				Map<String, String> serverMap
		) {
		}
	}
}
