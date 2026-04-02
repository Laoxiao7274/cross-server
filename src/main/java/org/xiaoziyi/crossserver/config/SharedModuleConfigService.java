package org.xiaoziyi.crossserver.config;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.model.GlobalSnapshot;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

public final class SharedModuleConfigService {
	public static final String NAMESPACE = "cluster.config";
	public static final String DATA_KEY = "modules.toggles";
	private static final int SCHEMA_VERSION = 1;

	private final Logger logger;
	private final CrossServerApi api;
	private final PluginConfiguration.ServerSettings serverSettings;

	public SharedModuleConfigService(Logger logger, CrossServerApi api, PluginConfiguration.ServerSettings serverSettings) {
		this.logger = logger;
		this.api = api;
		this.serverSettings = serverSettings;
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
		api.saveGlobalData(
				NAMESPACE,
				DATA_KEY,
				SharedModuleConfigCodec.encode(new SharedModuleConfigSnapshot(
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
						Instant.now()
				))
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
				Instant.now()
		), actorName);
	}

	private SharedModuleConfigSnapshot loadSharedSnapshot() {
		try {
			Optional<GlobalSnapshot> snapshot = api.loadGlobalData(NAMESPACE, DATA_KEY);
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
