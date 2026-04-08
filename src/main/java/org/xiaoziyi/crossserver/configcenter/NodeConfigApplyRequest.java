package org.xiaoziyi.crossserver.configcenter;

import java.time.Instant;
import java.util.Map;

public record NodeConfigApplyRequest(
		int schemaVersion,
		String requestId,
		String targetServerId,
		Map<String, Object> changes,
		String status,
		String requestedBy,
		Instant requestedAt,
		Instant appliedAt,
		String appliedBy,
		String resultMessage,
		String source,
		String summary
) {
}
