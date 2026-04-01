package org.xiaoziyi.crossserver.messaging;

import java.util.function.Consumer;

public final class NoopMessagingProvider implements MessagingProvider {
	@Override
	public void start() {
	}

	@Override
	public void publish(SyncMessage message) {
	}

	@Override
	public void registerListener(Consumer<SyncMessage> listener) {
	}

	@Override
	public void close() {
	}
}
