package org.xiaoziyi.crossserver.messaging;

import java.util.function.Consumer;

public interface MessagingProvider extends AutoCloseable {
	void start() throws Exception;

	void publish(SyncMessage message);

	void registerListener(Consumer<SyncMessage> listener);

	@Override
	void close() throws Exception;
}
