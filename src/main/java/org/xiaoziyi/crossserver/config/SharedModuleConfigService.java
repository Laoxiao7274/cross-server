package org.xiaoziyi.crossserver.config;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.configcenter.ConfigDocument;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentSchema;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentUpdate;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

public final class SharedModuleConfigService {
	public static final String NAMESPACE = "cluster.config";
	public static final String DATA_KEY = "modules.toggles";
	private static final int SCHEMA_VERSION = 1;
	private static final String SOURCE = "crossserver.modules";
	private static final ConfigDocumentSchema DOCUMENT_SCHEMA = new ConfigDocumentSchema(
			"SharedModuleConfig",
			1,
			java.util.List.of(),
			java.util.Map.of(
					"schemaVersion", "integer",
					"auth", "boolean",
					"homes", "boolean",
					"warps", "boolean",
					"tpa", "boolean",
					"routeConfig", "boolean",
					"transferAdmin", "boolean",
					"economyBridge", "boolean",
					"permissions", "boolean"
			),
			java.util.Map.of(),
			java.util.Map.of(),
			java.util.Map.of(),
			java.util.List.of(),
			java.util.Map.of(),
			true,
			null,
			"集群共享模块开关"
	);

	private final Logger logger;
	private final CrossServerApi api;
	private final PluginConfiguration.ServerSettings serverSettings;

	public SharedModuleConfigService(Logger logger, CrossServerApi api, PluginConfiguration.ServerSettings serverSettings) {
		this.logger = logger;
		this.api = api;
		this.serverSettings = serverSettings;
		this.api.registerConfigDocument(NAMESPACE, DATA_KEY, DOCUMENT_SCHEMA);
	}

	public PluginConfiguration mergeInto(PluginConfiguration configuration) {
		SharedModuleConfigSnapshot snapshot = loadSharedSnapshot();
		if (snapshot == null) {
			return configuration;
		}
		PluginConfiguration.ModuleSettings base = configuration.modules();
		return configuration.withModules(new PluginConfiguration.ModuleSettings(
				coalesce(snapshot.auth(), base.auth()),
				coalesce(snapshot.homes(), base.homes()),
				coalesce(snapshot.warps(), base.warps()),
				coalesce(snapshot.tpa(), base.tpa()),
				coalesce(snapshot.routeConfig(), base.routeConfig()),
				coalesce(snapshot.transferAdmin(), base.transferAdmin()),
				coalesce(snapshot.economyBridge(), base.economyBridge()),
				coalesce(snapshot.permissions(), base.permissions())
		));
	}

	public Optional<SharedModuleConfigSnapshot> loadSharedConfig() {
		return Optional.ofNullable(loadSharedSnapshot());
	}

	public void saveSharedConfig(SharedModuleConfigSnapshot snapshot, String actorName) throws Exception {
		SharedModuleConfigSnapshot normalized = new SharedModuleConfigSnapshot(
				SCHEMA_VERSION,
				snapshot.auth(),
				snapshot.homes(),
				snapshot.warps(),
				snapshot.tpa(),
				snapshot.routeConfig(),
				snapshot.transferAdmin(),
				snapshot.economyBridge(),
				snapshot.permissions(),
				normalizeActor(actorName),
				Instant.now(),
				SOURCE,
				"更新共享模块开关"
		);
		api.saveConfigDocument(
				NAMESPACE,
				DATA_KEY,
				new ConfigDocumentUpdate(
						SharedModuleConfigCodec.encode(normalized),
						SCHEMA_VERSION,
						normalized.updatedBy(),
						SOURCE,
						normalized.summary()
				)
		);
	}

	public void updateSharedModules(PluginConfiguration.ModuleSettings modules, String actorName) throws Exception {
		saveSharedConfig(new SharedModuleConfigSnapshot(
				SCHEMA_VERSION,
				modules.auth(),
				modules.homes(),
				modules.warps(),
				modules.tpa(),
				modules.routeConfig(),
				modules.transferAdmin(),
				modules.economyBridge(),
				modules.permissions(),
				normalizeActor(actorName),
				Instant.now(),
				SOURCE,
				"更新共享模块开关"
		), actorName);
	}

	private SharedModuleConfigSnapshot loadSharedSnapshot() {
		try {
			Optional<ConfigDocument> snapshot = api.loadConfigDocument(NAMESPACE, DATA_KEY);
			if (snapshot.isEmpty()) {
				return null;
			}
			return SharedModuleConfigCodec.decode(snapshot.get().payload());
		} catch (Exception exception) {
			logger.warning("加载共享模块开关配置失败，已回退到本地配置: " + exception.getMessage());
			return null;
		}
	}

	private boolean coalesce(Boolean override, boolean fallback) {
		return override != null ? override : fallback;
	}

	private String normalizeActor(String actorName) {
		if (actorName == null || actorName.isBlank()) {
			return serverSettings.id();
		}
		return actorName.trim();
	}
}
