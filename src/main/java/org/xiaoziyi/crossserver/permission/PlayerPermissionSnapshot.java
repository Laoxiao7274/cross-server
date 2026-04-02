package org.xiaoziyi.crossserver.permission;

import java.util.List;

public record PlayerPermissionSnapshot(
		List<String> permissions
) {
}
