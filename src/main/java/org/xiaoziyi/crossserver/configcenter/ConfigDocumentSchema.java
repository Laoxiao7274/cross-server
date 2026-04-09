package org.xiaoziyi.crossserver.configcenter;

import java.util.List;
import java.util.Map;

public record ConfigDocumentSchema(
		String name,
		int version,
		List<String> requiredPaths,
		Map<String, String> fieldTypes,
		boolean allowAdditionalFields,
		String examplePayload,
		String description
) {
	public ConfigDocumentSchema {
		requiredPaths = requiredPaths == null ? List.of() : List.copyOf(requiredPaths);
		fieldTypes = fieldTypes == null ? Map.of() : Map.copyOf(fieldTypes);
	}
}
