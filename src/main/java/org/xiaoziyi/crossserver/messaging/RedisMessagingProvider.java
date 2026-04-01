package org.xiaoziyi.crossserver.messaging;

import io.lettuce.core.RedisClient;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.xiaoziyi.crossserver.config.PluginConfiguration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class RedisMessagingProvider implements MessagingProvider {
	private final Logger logger;
	private final PluginConfiguration.MessagingSettings settings;
	private final MessageCodec codec;
	private final List<Consumer<SyncMessage>> listeners;
	private RedisClient client;
	private StatefulRedisConnection<String, String> commandConnection;
	private StatefulRedisPubSubConnection<String, String> pubSubConnection;
	private RedisCommands<String, String> commands;

	public RedisMessagingProvider(Logger logger, PluginConfiguration.MessagingSettings settings) {
		this.logger = logger;
		this.settings = settings;
		this.codec = new MessageCodec();
		this.listeners = new CopyOnWriteArrayList<>();
	}

	@Override
	public void start() {
		this.client = RedisClient.create(settings.redisUri());
		this.commandConnection = client.connect();
		this.pubSubConnection = client.connectPubSub();
		this.commands = commandConnection.sync();
		this.pubSubConnection.addListener(new RedisPubSubAdapter<>() {
			@Override
			public void message(String channel, String message) {
				if (!settings.channel().equals(channel)) {
					return;
				}
				dispatch(codec.decode(message));
			}
		});
		RedisPubSubCommands<String, String> pubSubCommands = pubSubConnection.sync();
		pubSubCommands.subscribe(settings.channel());
		logger.info("Redis 消息总线已连接，频道: " + settings.channel());
	}

	@Override
	public void publish(SyncMessage message) {
		if (commands == null) {
			return;
		}
		commands.publish(settings.channel(), codec.encode(message));
	}

	@Override
	public void registerListener(Consumer<SyncMessage> listener) {
		listeners.add(listener);
	}

	@Override
	public void close() {
		if (pubSubConnection != null) {
			pubSubConnection.close();
		}
		if (commandConnection != null) {
			commandConnection.close();
		}
		if (client != null) {
			client.shutdown();
		}
	}

	private void dispatch(SyncMessage message) {
		for (Consumer<SyncMessage> listener : listeners) {
			try {
				listener.accept(message);
			} catch (Exception exception) {
				logger.warning("处理同步消息失败: " + exception.getMessage());
			}
		}
	}
}
