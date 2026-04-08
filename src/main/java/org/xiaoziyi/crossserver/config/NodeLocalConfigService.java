package org.xiaoziyi.crossserver.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.xiaoziyi.crossserver.bootstrap.CrossServerPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class NodeLocalConfigService {
	private final CrossServerPlugin plugin;

	public NodeLocalConfigService(CrossServerPlugin plugin) {
		this.plugin = plugin;
	}

	public Map<String, Object> exportEditableConfig(PluginConfiguration configuration) {
		Objects.requireNonNull(configuration, "configuration");
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("server", Map.of(
				"id", configuration.server().id(),
				"cluster", configuration.server().cluster()
		));
		result.put("messaging", Map.of(
				"enabled", configuration.messaging().enabled(),
				"redisUri", configuration.messaging().redisUri(),
				"channel", configuration.messaging().channel()
		));
		Map<String, Object> webPanel = new LinkedHashMap<>();
		webPanel.put("enabled", configuration.webPanel().enabled());
		webPanel.put("host", configuration.webPanel().host());
		webPanel.put("port", configuration.webPanel().port());
		webPanel.put("masterServerId", configuration.webPanel().masterServerId());
		webPanel.put("tokenConfigured", configuration.webPanel().token() != null && !configuration.webPanel().token().isBlank());
		result.put("webPanel", webPanel);
		result.put("modules", Map.of(
				"auth", configuration.modules().auth(),
				"homes", configuration.modules().homes(),
				"warps", configuration.modules().warps(),
				"tpa", configuration.modules().tpa(),
				"routeConfig", configuration.modules().routeConfig(),
				"transferAdmin", configuration.modules().transferAdmin(),
				"economyBridge", configuration.modules().economyBridge(),
				"permissions", configuration.modules().permissions()
		));
		return result;
	}

	public Map<String, Object> normalizeChanges(Map<String, Object> requestChanges) {
		Objects.requireNonNull(requestChanges, "requestChanges");
		Map<String, Object> normalized = new LinkedHashMap<>();
		Map<String, Object> messaging = normalizeMessaging(optionalMap(requestChanges.get("messaging"), "messaging"));
		if (!messaging.isEmpty()) {
			normalized.put("messaging", messaging);
		}
		Map<String, Object> webPanel = normalizeWebPanel(optionalMap(requestChanges.get("webPanel"), "webPanel"));
		if (!webPanel.isEmpty()) {
			normalized.put("webPanel", webPanel);
		}
		Map<String, Object> modules = normalizeModules(optionalMap(requestChanges.get("modules"), "modules"));
		if (!modules.isEmpty()) {
			normalized.put("modules", modules);
		}
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("未提供可编辑的节点配置变更");
		}
		return normalized;
	}

	public void applyChanges(Map<String, Object> requestChanges) throws Exception {
		Map<String, Object> changes = normalizeChanges(requestChanges);
		File configFile = new File(plugin.getDataFolder(), "config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
		applyMessaging(yaml, optionalMap(changes.get("messaging"), "messaging"));
		applyWebPanel(yaml, optionalMap(changes.get("webPanel"), "webPanel"));
		applyModules(yaml, optionalMap(changes.get("modules"), "modules"));
		yaml.save(configFile);
	}

	private void applyMessaging(YamlConfiguration yaml, Map<String, Object> messaging) {
		if (messaging.isEmpty()) {
			return;
		}
		setIfPresent(yaml, "messaging.enabled", messaging.get("enabled"));
		setIfPresent(yaml, "messaging.redis-uri", messaging.get("redisUri"));
		setIfPresent(yaml, "messaging.channel", messaging.get("channel"));
	}

	private void applyWebPanel(YamlConfiguration yaml, Map<String, Object> webPanel) {
		if (webPanel.isEmpty()) {
			return;
		}
		setIfPresent(yaml, "web-panel.enabled", webPanel.get("enabled"));
		setIfPresent(yaml, "web-panel.host", webPanel.get("host"));
		setIfPresent(yaml, "web-panel.port", webPanel.get("port"));
		setIfPresent(yaml, "web-panel.master-server-id", webPanel.get("masterServerId"));
		setIfPresent(yaml, "web-panel.token", webPanel.get("token"));
	}

	private void applyModules(YamlConfiguration yaml, Map<String, Object> modules) {
		if (modules.isEmpty()) {
			return;
		}
		setIfPresent(yaml, "modules.auth", modules.get("auth"));
		setIfPresent(yaml, "modules.homes", modules.get("homes"));
		setIfPresent(yaml, "modules.warps", modules.get("warps"));
		setIfPresent(yaml, "modules.tpa", modules.get("tpa"));
		setIfPresent(yaml, "modules.route-config", modules.get("routeConfig"));
		setIfPresent(yaml, "modules.transfer-admin", modules.get("transferAdmin"));
		setIfPresent(yaml, "modules.economy-bridge", modules.get("economyBridge"));
		setIfPresent(yaml, "modules.permissions", modules.get("permissions"));
	}

	private void setIfPresent(YamlConfiguration yaml, String path, Object value) {
		if (value != null) {
			yaml.set(path, value);
		}
	}

	private Map<String, Object> normalizeMessaging(Map<String, Object> source) {
		Map<String, Object> normalized = new LinkedHashMap<>();
		putBoolean(source, normalized, "enabled");
		putString(source, normalized, "redisUri");
		putString(source, normalized, "channel");
		return normalized;
	}

	private Map<String, Object> normalizeWebPanel(Map<String, Object> source) {
		Map<String, Object> normalized = new LinkedHashMap<>();
		putBoolean(source, normalized, "enabled");
		putString(source, normalized, "host");
		putPort(source, normalized, "port");
		putString(source, normalized, "masterServerId");
		putOptionalString(source, normalized, "token");
		return normalized;
	}

	private Map<String, Object> normalizeModules(Map<String, Object> source) {
		Map<String, Object> normalized = new LinkedHashMap<>();
		putBoolean(source, normalized, "auth");
		putBoolean(source, normalized, "homes");
		putBoolean(source, normalized, "warps");
		putBoolean(source, normalized, "tpa");
		putBoolean(source, normalized, "routeConfig");
		putBoolean(source, normalized, "transferAdmin");
		putBoolean(source, normalized, "economyBridge");
		putBoolean(source, normalized, "permissions");
		return normalized;
	}

	private void putBoolean(Map<String, Object> source, Map<String, Object> target, String field) {
		if (!source.containsKey(field)) {
			return;
		}
		Object value = source.get(field);
		if (!(value instanceof Boolean bool)) {
			throw new IllegalArgumentException(field + " 必须是布尔值");
		}
		target.put(field, bool);
	}

	private void putString(Map<String, Object> source, Map<String, Object> target, String field) {
		if (!source.containsKey(field)) {
			return;
		}
		Object value = source.get(field);
		if (!(value instanceof String text) || text.isBlank()) {
			throw new IllegalArgumentException(field + " 必须是非空字符串");
		}
		target.put(field, text.trim());
	}

	private void putOptionalString(Map<String, Object> source, Map<String, Object> target, String field) {
		if (!source.containsKey(field)) {
			return;
		}
		Object value = source.get(field);
		if (!(value instanceof String text) || text.isBlank()) {
			throw new IllegalArgumentException(field + " 必须是非空字符串");
		}
		target.put(field, text.trim());
	}

	private void putPort(Map<String, Object> source, Map<String, Object> target, String field) {
		if (!source.containsKey(field)) {
			return;
		}
		Object value = source.get(field);
		if (!(value instanceof Number number)) {
			throw new IllegalArgumentException(field + " 必须是数字");
		}
		int port = number.intValue();
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException(field + " 必须在 1-65535 之间");
		}
		target.put(field, port);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> optionalMap(Object value, String field) {
		if (value == null) {
			return Map.of();
		}
		if (!(value instanceof Map<?, ?> raw)) {
			throw new IllegalArgumentException(field + " 必须是对象");
		}
		Map<String, Object> normalized = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : raw.entrySet()) {
			if (!(entry.getKey() instanceof String key)) {
				throw new IllegalArgumentException(field + " 的键必须是字符串");
			}
			normalized.put(key, entry.getValue());
		}
		return normalized;
	}
}
