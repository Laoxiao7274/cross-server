package org.xiaoziyi.crossserver.configcenter;

public record ConfigDocumentUpdate(
		String payload,
		int schemaVersion,
		String updatedBy,
		String source,
		String summary
) {
}
