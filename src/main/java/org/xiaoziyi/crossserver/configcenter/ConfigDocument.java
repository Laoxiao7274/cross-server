package org.xiaoziyi.crossserver.configcenter;

import java.time.Instant;

public record ConfigDocument(
		String namespace,
		String dataKey,
		String payload,
		ConfigDocumentFormat format,
		long version,
		int schemaVersion,
		String updatedBy,
		Instant updatedAt,
		String source,
		String summary
) {
}
