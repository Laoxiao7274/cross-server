package org.xiaoziyi.crossserver.configcenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.messaging.SyncMessage;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class ConfigCenterService {
	private static final String HISTORY_NAMESPACE = "config.history";
	private static final int MAX_HISTORY_ENTRIES = 20;
	private static final ObjectMapper OBJECT_MAPPER = ConfigCenterObjectMapper.create();

	private final CrossServerApi api;
	private final Set<RegisteredConfigDocument> registeredDocuments = Collections.synchronizedSet(new LinkedHashSet<>());

	public ConfigCenterService(CrossServerApi api) {
		this.api = api;
	}

	public void registerDocument(String namespace, String dataKey) {
		registerDocument(namespace, dataKey, null);
	}

	public void registerDocument(String namespace, String dataKey, ConfigDocumentSchema schema) {
		String normalizedNamespace = requireText(namespace, "namespace");
		String normalizedDataKey = requireText(dataKey, "dataKey");
		api.registerNamespace(normalizedNamespace);
		registeredDocuments.add(new RegisteredConfigDocument(normalizedNamespace, normalizedDataKey, schema));
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

	public List<Map<String, Object>> loadDocumentHistory(String namespace, String dataKey) throws Exception {
		String historyKey = historyKey(requireText(namespace, "namespace"), requireText(dataKey, "dataKey"));
		Optional<GlobalSnapshot> snapshot = api.loadGlobalData(HISTORY_NAMESPACE, historyKey);
		if (snapshot.isEmpty()) {
			return List.of();
		}
		Object value = OBJECT_MAPPER.readValue(snapshot.get().payload(), Object.class);
		if (!(value instanceof Map<?, ?> raw)) {
			return List.of();
		}
		Object entries = raw.get("entries");
		if (!(entries instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> result = new java.util.ArrayList<>();
		for (Object item : list) {
			if (item instanceof Map<?, ?> rawItem) {
				Map<String, Object> normalized = new LinkedHashMap<>();
				for (Map.Entry<?, ?> entry : rawItem.entrySet()) {
					if (entry.getKey() instanceof String key) {
						normalized.put(key, entry.getValue());
					}
				}
				result.add(normalized);
			}
		}
		return List.copyOf(result);
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
		findRegisteredDocument(normalizedNamespace, normalizedDataKey).ifPresent(document -> ConfigDocumentSchemaValidator.validate(payload, document.schema()));
		GlobalSnapshot snapshot = api.saveGlobalData(normalizedNamespace, normalizedDataKey, payload);
		registeredDocuments.add(copyRegistration(normalizedNamespace, normalizedDataKey));
		ConfigDocument saved = ConfigDocumentCodec.fromSnapshot(snapshot);
		recordHistory(saved);
		return saved;
	}

	public ConfigDocument saveDocument(String namespace, String dataKey, String payload) throws Exception {
		String normalizedNamespace = requireText(namespace, "namespace");
		String normalizedDataKey = requireText(dataKey, "dataKey");
		findRegisteredDocument(normalizedNamespace, normalizedDataKey).ifPresent(document -> ConfigDocumentSchemaValidator.validate(payload, document.schema()));
		GlobalSnapshot snapshot = api.saveGlobalData(normalizedNamespace, normalizedDataKey, requireText(payload, "payload"));
		registeredDocuments.add(copyRegistration(normalizedNamespace, normalizedDataKey));
		ConfigDocument saved = ConfigDocumentCodec.fromSnapshot(snapshot);
		recordHistory(saved);
		return saved;
	}

	public Runnable registerDocumentListener(String namespace, String dataKey, Consumer<ConfigChangeEvent> listener) {
		String normalizedNamespace = requireText(namespace, "namespace");
		String normalizedDataKey = requireText(dataKey, "dataKey");
		Objects.requireNonNull(listener, "listener");
		registeredDocuments.add(copyRegistration(normalizedNamespace, normalizedDataKey));
		return api.registerSyncListenerHandle(normalizedNamespace, message -> notifyDocumentListener(normalizedDataKey, listener, message));
	}

	private Optional<RegisteredConfigDocument> findRegisteredDocument(String namespace, String dataKey) {
		synchronized (registeredDocuments) {
			return registeredDocuments.stream()
					.filter(document -> namespace.equals(document.namespace()) && dataKey.equals(document.dataKey()))
					.findFirst();
		}
	}

	private RegisteredConfigDocument copyRegistration(String namespace, String dataKey) {
		return findRegisteredDocument(namespace, dataKey)
				.orElseGet(() -> new RegisteredConfigDocument(namespace, dataKey, null));
	}

	private void recordHistory(ConfigDocument document) throws Exception {
		if (document == null || HISTORY_NAMESPACE.equalsIgnoreCase(document.namespace())) {
			return;
		}
		String key = historyKey(document.namespace(), document.dataKey());
		List<Map<String, Object>> entries = new java.util.ArrayList<>(loadDocumentHistory(document.namespace(), document.dataKey()));
		Map<String, Object> historyItem = new LinkedHashMap<>();
		historyItem.put("namespace", document.namespace());
		historyItem.put("dataKey", document.dataKey());
		historyItem.put("version", document.version());
		historyItem.put("schemaVersion", document.schemaVersion());
		historyItem.put("format", document.format() == null ? null : document.format().name().toLowerCase(java.util.Locale.ROOT));
		historyItem.put("updatedBy", document.updatedBy());
		historyItem.put("updatedAt", document.updatedAt());
		historyItem.put("source", document.source());
		historyItem.put("summary", document.summary());
		historyItem.put("payload", document.payload());
		entries.removeIf(item -> java.util.Objects.equals(item.get("version"), document.version()));
		entries.add(0, historyItem);
		if (entries.size() > MAX_HISTORY_ENTRIES) {
			entries = new java.util.ArrayList<>(entries.subList(0, MAX_HISTORY_ENTRIES));
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entries", entries);
		api.saveGlobalData(HISTORY_NAMESPACE, key, OBJECT_MAPPER.writeValueAsString(payload));
	}

	private String historyKey(String namespace, String dataKey) {
		return namespace + "::" + dataKey;
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
