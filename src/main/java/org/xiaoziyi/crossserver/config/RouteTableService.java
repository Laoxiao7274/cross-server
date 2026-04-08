package org.xiaoziyi.crossserver.config;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.configcenter.ConfigDocument;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentUpdate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class RouteTableService {
	public static final String NAMESPACE = "cluster.config";
	public static final String DATA_KEY = "teleport.routes";
	private static final int SCHEMA_VERSION = 1;
	private static final String SOURCE = "crossserver.routes";

	private final Logger logger;
	private final CrossServerApi api;
	private final PluginConfiguration.ServerSettings serverSettings;

	public RouteTableService(Logger logger, CrossServerApi api, PluginConfiguration.ServerSettings serverSettings) {
		this.logger = logger;
		this.api = api;
		this.serverSettings = serverSettings;
		this.api.registerConfigDocument(NAMESPACE, DATA_KEY);
	}

	public PluginConfiguration mergeInto(PluginConfiguration configuration) {
		Map<String, String> merged = new LinkedHashMap<>(configuration.teleport().gateway().serverMap());
		for (Map.Entry<String, String> entry : loadSharedRoutes().entrySet()) {
			String serverId = normalize(entry.getKey());
			String proxyTarget = normalize(entry.getValue());
			if (serverId == null || proxyTarget == null) {
				logger.warning("忽略非法共享路由配置: " + entry);
				continue;
			}
			merged.put(serverId, proxyTarget);
		}
		return configuration.withTeleportGatewayServerMap(merged);
	}

	public Map<String, String> loadSharedRoutes() {
		try {
			Optional<ConfigDocument> snapshot = api.loadConfigDocument(NAMESPACE, DATA_KEY);
			if (snapshot.isEmpty()) {
				return Map.of();
			}
			RouteTableSnapshot routeTableSnapshot = RouteConfigCodec.decode(snapshot.get().payload());
			if (routeTableSnapshot.routes() == null || routeTableSnapshot.routes().isEmpty()) {
				return Map.of();
			}
			Map<String, String> routes = new LinkedHashMap<>();
			for (Map.Entry<String, String> entry : routeTableSnapshot.routes().entrySet()) {
				String serverId = normalize(entry.getKey());
				String proxyTarget = normalize(entry.getValue());
				if (serverId == null || proxyTarget == null) {
					logger.warning("共享路由表存在非法项，已忽略: " + entry);
					continue;
				}
				routes.put(serverId, proxyTarget);
			}
			return Map.copyOf(routes);
		} catch (Exception exception) {
			logger.warning("加载共享路由表失败，已回退到本地配置: " + exception.getMessage());
			return Map.of();
		}
	}

	public void saveSharedRoutes(Map<String, String> routes, String actorName) throws Exception {
		Map<String, String> sanitized = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : routes.entrySet()) {
			String serverId = require(entry.getKey(), "serverId");
			String proxyTarget = require(entry.getValue(), "proxyTarget");
			sanitized.put(serverId, proxyTarget);
		}
		RouteTableSnapshot snapshot = new RouteTableSnapshot(
				SCHEMA_VERSION,
				Map.copyOf(sanitized),
				normalizeActor(actorName),
				Instant.now(),
				SOURCE,
				"更新共享路由表"
		);
		api.saveConfigDocument(
				NAMESPACE,
				DATA_KEY,
				new ConfigDocumentUpdate(
						RouteConfigCodec.encode(snapshot),
						SCHEMA_VERSION,
						snapshot.updatedBy(),
						SOURCE,
						snapshot.summary()
				)
		);
	}

	public Map<String, String> mergedRoutes(PluginConfiguration configuration) {
		return mergeInto(configuration).teleport().gateway().serverMap();
	}

	public void setSharedRoute(String serverId, String proxyTarget, String actorName) throws Exception {
		Map<String, String> routes = new LinkedHashMap<>(loadSharedRoutes());
		routes.put(require(serverId, "serverId"), require(proxyTarget, "proxyTarget"));
		saveSharedRoutes(routes, actorName);
	}

	public boolean removeSharedRoute(String serverId, String actorName) throws Exception {
		String normalizedServerId = require(serverId, "serverId");
		Map<String, String> routes = new LinkedHashMap<>(loadSharedRoutes());
		if (routes.remove(normalizedServerId) == null) {
			return false;
		}
		saveSharedRoutes(routes, actorName);
		return true;
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private String require(String value, String field) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new IllegalStateException("共享路由配置字段不能为空: " + field);
		}
		return normalized;
	}

	private String normalizeActor(String actorName) {
		String normalized = normalize(actorName);
		return normalized == null ? serverSettings.id() : normalized;
	}
}
