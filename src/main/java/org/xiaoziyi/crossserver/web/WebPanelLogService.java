package org.xiaoziyi.crossserver.web;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.configcenter.ConfigDocument;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentUpdate;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class WebPanelLogService implements AutoCloseable {
	public static final String NAMESPACE = "cluster.config";
	private static final String DATA_KEY_PREFIX = "web.panel.logs.";
	private static final int SCHEMA_VERSION = 1;
	private static final String SOURCE = "crossserver.web-panel.logs";
	private static final int MAX_BUFFER_SIZE = 200;
	private static final int MAX_SYNC_SIZE = 120;

	private final CrossServerApi api;
	private final PluginConfiguration.ServerSettings serverSettings;
	private final Logger logger;
	private final ConcurrentLinkedDeque<WebPanelLogEntry> entries = new ConcurrentLinkedDeque<>();
	private final Handler handler = new Handler() {
		@Override
		public void publish(LogRecord record) {
			if (record == null || !isLoggable(record)) {
				return;
			}
			String message = formatMessage(record);
			if (message == null || message.isBlank()) {
				return;
			}
			append(new WebPanelLogEntry(
					record.getInstant() == null ? Instant.now() : record.getInstant(),
					record.getLevel() == null ? "INFO" : record.getLevel().getName(),
					message
			));
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	};

	public WebPanelLogService(CrossServerApi api, PluginConfiguration.ServerSettings serverSettings, Logger logger) {
		this.api = api;
		this.serverSettings = serverSettings;
		this.logger = logger;
		this.api.registerConfigDocument(NAMESPACE, dataKey(serverSettings.id()));
		this.handler.setLevel(Level.INFO);
		this.logger.addHandler(handler);
	}

	public void captureStartupMessage(String message) {
		if (message == null || message.isBlank()) {
			return;
		}
		append(new WebPanelLogEntry(Instant.now(), "INFO", message.trim()));
	}

	public void publishSnapshot() throws Exception {
		List<WebPanelLogEntry> snapshotEntries = latestEntries(MAX_SYNC_SIZE);
		WebPanelLogSnapshot snapshot = new WebPanelLogSnapshot(
				SCHEMA_VERSION,
				serverSettings.id(),
				List.copyOf(snapshotEntries),
				Instant.now(),
				serverSettings.id(),
				SOURCE,
				"同步 Web 面板最近日志"
		);
		api.saveConfigDocument(
				NAMESPACE,
				dataKey(serverSettings.id()),
				new ConfigDocumentUpdate(
						WebPanelLogCodec.encode(snapshot),
						SCHEMA_VERSION,
						serverSettings.id(),
						SOURCE,
						snapshot.summary()
				)
		);
	}

	public Optional<WebPanelLogSnapshot> loadSnapshot(String serverId) {
		try {
			Optional<ConfigDocument> snapshot = api.loadConfigDocument(NAMESPACE, dataKey(serverId));
			if (snapshot.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(WebPanelLogCodec.decode(snapshot.get().payload()));
		} catch (Exception exception) {
			return Optional.empty();
		}
	}

	private void append(WebPanelLogEntry entry) {
		entries.addLast(entry);
		while (entries.size() > MAX_BUFFER_SIZE) {
			entries.pollFirst();
		}
	}

	private List<WebPanelLogEntry> latestEntries(int limit) {
		List<WebPanelLogEntry> result = new ArrayList<>();
		int skipped = Math.max(0, entries.size() - Math.max(1, limit));
		int index = 0;
		for (WebPanelLogEntry entry : entries) {
			if (index++ < skipped) {
				continue;
			}
			result.add(entry);
		}
		return result;
	}

	private String dataKey(String serverId) {
		return DATA_KEY_PREFIX + serverId;
	}

	private String formatMessage(LogRecord record) {
		String message = record.getMessage();
		Object[] parameters = record.getParameters();
		if (message != null && parameters != null && parameters.length > 0) {
			try {
				message = MessageFormat.format(message, parameters);
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (record.getThrown() == null) {
			return message;
		}
		String exceptionText = record.getThrown().getClass().getSimpleName()
				+ (record.getThrown().getMessage() == null || record.getThrown().getMessage().isBlank() ? "" : ": " + record.getThrown().getMessage());
		if (message == null || message.isBlank()) {
			return exceptionText;
		}
		return message + " | " + exceptionText;
	}

	@Override
	public void close() {
		logger.removeHandler(handler);
	}
}
