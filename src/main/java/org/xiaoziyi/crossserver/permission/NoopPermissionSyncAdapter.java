package org.xiaoziyi.crossserver.permission;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NoopPermissionSyncAdapter implements PermissionSyncAdapter {
	private static final Set<String> MANAGED_PREFIXES = Set.of("crossserver.");

	private final Plugin plugin;
	private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

	public NoopPermissionSyncAdapter(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public Collection<String> capturePermissions(Player player) {
		Set<String> permissions = new LinkedHashSet<>();
		for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
			String permission = info.getPermission();
			if (info.getValue() && isManaged(permission)) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	@Override
	public void applyPermissions(Player player, Collection<String> permissions) {
		PermissionAttachment attachment = attachments.computeIfAbsent(player.getUniqueId(), key -> player.addAttachment(plugin));
		clearManagedPermissions(attachment);
		for (String permission : permissions) {
			if (permission != null && !permission.isBlank() && isManaged(permission)) {
				attachment.setPermission(permission, true);
			}
		}
		player.recalculatePermissions();
	}

	@Override
	public void clearPermissions(Player player) {
		PermissionAttachment attachment = attachments.remove(player.getUniqueId());
		if (attachment == null) {
			return;
		}
		player.removeAttachment(attachment);
		player.recalculatePermissions();
	}

	@Override
	public String name() {
		return "attachment(crossserver.* only)";
	}

	private boolean isManaged(String permission) {
		for (String prefix : MANAGED_PREFIXES) {
			if (permission.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private void clearManagedPermissions(PermissionAttachment attachment) {
		for (String permission : Set.copyOf(attachment.getPermissions().keySet())) {
			if (isManaged(permission)) {
				attachment.unsetPermission(permission);
			}
		}
	}
}
