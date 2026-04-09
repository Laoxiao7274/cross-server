package org.xiaoziyi.crossserver.configcenter;

public record RegisteredConfigDocument(
		String namespace,
		String dataKey,
		ConfigDocumentSchema schema
) {
}
