package org.xiaoziyi.crossserver.permission;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface PermissionSyncAdapter {
	Collection<String> capturePermissions(Player player);

	void applyPermissions(Player player, Collection<String> permissions);

	void clearPermissions(Player player);

	String name();
}
