package org.xiaoziyi.crossserver.web;

import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.xiaoziyi.crossserver.config.PluginConfiguration;
import org.xiaoziyi.crossserver.configcenter.ConfigDocument;
import org.xiaoziyi.crossserver.configcenter.ConfigDocumentUpdate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class WebPanelClusterService {
	public static final String NAMESPACE = "cluster.config";
	public static final String DATA_KEY = "web.panel.cluster";
	private static final int SCHEMA_VERSION = 1;
	private static final String SOURCE = "crossserver.web-panel";

	private final CrossServerApi api;
	private final PluginConfiguration.ServerSettings serverSettings;
	private final PluginConfiguration.WebPanelSettings settings;

	public WebPanelClusterService(CrossServerApi api, PluginConfiguration.ServerSettings serverSettings, PluginConfiguration.WebPanelSettings settings) {
		this.api = api;
		this.serverSettings = serverSettings;
		this.settings = settings;
		this.api.registerConfigDocument(NAMESPACE, DATA_KEY);
	}

	public WebPanelClusterSnapshot heartbeat() throws Exception {
		Instant now = Instant.now();
		Map<String, WebPanelMemberSnapshot> members = new LinkedHashMap<>(loadSnapshot().map(WebPanelClusterSnapshot::members).orElse(Map.of()));
		pruneExpiredMembers(members, now);
		members.put(serverSettings.id(), localMember(now));
		WebPanelClusterSnapshot snapshot = new WebPanelClusterSnapshot(
				SCHEMA_VERSION,
				settings.masterServerId(),
				Map.copyOf(members),
				now,
				serverSettings.id(),
				SOURCE,
				"同步 Web 面板集群节点状态"
		);
		api.saveConfigDocument(
				NAMESPACE,
				DATA_KEY,
				new ConfigDocumentUpdate(
						WebPanelClusterCodec.encode(snapshot),
						SCHEMA_VERSION,
						serverSettings.id(),
						SOURCE,
						snapshot.summary()
				)
		);
		return snapshot;
	}

	public Optional<WebPanelClusterSnapshot> loadSnapshot() throws Exception {
		Optional<ConfigDocument> snapshot = api.loadConfigDocument(NAMESPACE, DATA_KEY);
		if (snapshot.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(WebPanelClusterCodec.decode(snapshot.get().payload()));
	}

	public boolean shouldHost(WebPanelClusterSnapshot snapshot) {
		if (!settings.enabled()) {
			return false;
		}
		String masterServerId = snapshot != null && snapshot.leaderServerId() != null && !snapshot.leaderServerId().isBlank()
				? snapshot.leaderServerId()
				: settings.masterServerId();
		return masterServerId != null && masterServerId.equalsIgnoreCase(serverSettings.id());
	}

	public void unregisterLocalMember() throws Exception {
		Instant now = Instant.now();
		Map<String, WebPanelMemberSnapshot> members = new LinkedHashMap<>(loadSnapshot().map(WebPanelClusterSnapshot::members).orElse(Map.of()));
		pruneExpiredMembers(members, now);
		members.remove(serverSettings.id());
		WebPanelClusterSnapshot snapshot = new WebPanelClusterSnapshot(
				SCHEMA_VERSION,
				settings.masterServerId(),
				Map.copyOf(members),
				now,
				serverSettings.id(),
				SOURCE,
				"注销 Web 面板节点"
		);
		api.saveConfigDocument(
				NAMESPACE,
				DATA_KEY,
				new ConfigDocumentUpdate(
						WebPanelClusterCodec.encode(snapshot),
						SCHEMA_VERSION,
						serverSettings.id(),
						SOURCE,
						snapshot.summary()
				)
		);
	}

	private void pruneExpiredMembers(Map<String, WebPanelMemberSnapshot> members, Instant now) {
		long expirySeconds = Math.max(10L, settings.clusterLeaseSeconds());
		members.entrySet().removeIf(entry -> {
			WebPanelMemberSnapshot member = entry.getValue();
			return member == null || member.advertisedAt() == null || member.advertisedAt().plusSeconds(expirySeconds).isBefore(now);
		});
	}

	private WebPanelMemberSnapshot localMember(Instant now) {
		return new WebPanelMemberSnapshot(
				serverSettings.id(),
				settings.host(),
				settings.port(),
				settings.isMasterServer(serverSettings.id()),
				now
		);
	}
}
