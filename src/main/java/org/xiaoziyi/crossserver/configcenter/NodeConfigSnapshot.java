package org.xiaoziyi.crossserver.configcenter;

import java.time.Instant;
import java.util.Map;

public record NodeConfigSnapshot(
		int schemaVersion,
		String serverId,
		Map<String, Object> editableConfig,
		Instant capturedAt,
		String updatedBy,
		String source,
		String summary
) {
}
