package org.xiaoziyi.crossserver.web;

import java.time.Instant;
import java.util.List;

public record WebPanelLogSnapshot(
		int schemaVersion,
		String serverId,
		List<WebPanelLogEntry> entries,
		Instant updatedAt,
		String updatedBy,
		String source,
		String summary
) {
}
