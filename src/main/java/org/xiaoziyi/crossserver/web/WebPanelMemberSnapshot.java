package org.xiaoziyi.crossserver.web;

import java.time.Instant;

public record WebPanelMemberSnapshot(
		String serverId,
		String host,
		int port,
		boolean master,
		Instant advertisedAt
) {
}
