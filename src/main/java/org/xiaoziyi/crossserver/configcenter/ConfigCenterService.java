package org.xiaoziyi.crossserver.configcenter;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.messaging.SyncMessage;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class ConfigCenterService {
	private final CrossServerApi api;
	private final Set<RegisteredConfigDocument> registeredDocuments = Collections.synchronizedSet(new LinkedHashSet<>());

	public ConfigCenterService(CrossServerApi api) {
		this.api = api;
	}

	public void registerDocument(String namespace, String dataKey) {
		String normalizedNamespace = requireText(namespace, "namespace");
		String normalizedDataKey = requireText(dataKey, "dataKey");
		api.registerNamespace(normalizedNamespace);
		registeredDocuments.add(new RegisteredConfigDocument(normalizedNamespace, normalizedDataKey));
	}

	public Set<RegisteredConfigDocument> getRegisteredDocuments() {
		synchronized (registeredDocuments) {
			return Set.copyOf(registeredDocuments);
		}
	}

	public Optional<ConfigEntry> loadEntry(String namespace, String dataKey) throws Exception {
		Optional<GlobalSnapshot> snapshot = api.loadGlobalData(requireText(namespace, "namespace"), requireText(dataKey, "dataKey"));
		if (snapshot.isEmpty()) {
			return Optional.empty();
		}
		GlobalSnapshot value = snapshot.get();
		return Optional.of(new ConfigEntry(value.namespace(), value.dataKey(), value.payload(), value.version(), value.updatedAt()));
	}

	public Optional<ConfigDocument> loadDocument(String namespace, String dataKey) throws Exception {
		return loadEntry(namespace, dataKey)
				.map(entry -> ConfigDocumentCodec.fromSnapshot(entry.namespace(), entry.dataKey(), entry.payload(), entry.version(), entry.updatedAt()));
	}

	public ConfigDocument saveDocument(String namespace, String dataKey, ConfigDocumentUpdate update) throws Exception {
		String normalizedNamespace = requireText(namespace, "namespace");
		String normalizedDataKey = requireText(dataKey, "dataKey");
		Objects.requireNonNull(update, "update");
		String payload = ConfigDocumentPayloadFactory.enrich(
				requirePayload(update),
				update.schemaVersion(),
				update.updatedBy(),
				update.source(),
				update.summary()
		);
		GlobalSnapshot snapshot = api.saveGlobalData(normalizedNamespace, normalizedDataKey, payload);
		registeredDocuments.add(new RegisteredConfigDocument(normalizedNamespace, normalizedDataKey));
		return ConfigDocumentCodec.fromSnapshot(snapshot);
	}

	public ConfigDocument saveDocument(String namespace, String dataKey, String payload) throws Exception {
		String normalizedNamespace = requireText(namespace, "namespace");
		String normalizedDataKey = requireText(dataKey, "dataKey");
		GlobalSnapshot snapshot = api.saveGlobalData(normalizedNamespace, normalizedDataKey, requireText(payload, "payload"));
		registeredDocuments.add(new RegisteredConfigDocument(normalizedNamespace, normalizedDataKey));
		return ConfigDocumentCodec.fromSnapshot(snapshot);
	}

	public Runnable registerDocumentListener(String namespace, String dataKey, Consumer<ConfigChangeEvent> listener) {
		String normalizedNamespace = requireText(namespace, "namespace");
		String normalizedDataKey = requireText(dataKey, "dataKey");
		Objects.requireNonNull(listener, "listener");
		registeredDocuments.add(new RegisteredConfigDocument(normalizedNamespace, normalizedDataKey));
		return api.registerSyncListenerHandle(normalizedNamespace, message -> notifyDocumentListener(normalizedDataKey, listener, message));
	}

	private void notifyDocumentListener(String dataKey, Consumer<ConfigChangeEvent> listener, SyncMessage message) {
		if (!"global".equalsIgnoreCase(message.targetType())) {
			return;
		}
		if (!dataKey.equalsIgnoreCase(message.targetId())) {
			return;
		}
		listener.accept(new ConfigChangeEvent(
				message.namespace(),
				message.targetId(),
				message.version(),
				message.sourceServerId(),
				message.occurredAt() == null ? Instant.now() : message.occurredAt()
		));
	}

	private String requirePayload(ConfigDocumentUpdate update) {
		return requireText(update.payload(), "payload");
	}

	private String requireText(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("配置中心字段不能为空: " + field);
		}
		return value.trim();
	}
}
