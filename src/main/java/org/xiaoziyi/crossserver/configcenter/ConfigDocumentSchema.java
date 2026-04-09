package org.xiaoziyi.crossserver.configcenter;

import java.util.List;
import java.util.Map;

public record ConfigDocumentSchema(
		String name,
		int version,
		List<String> requiredPaths,
		Map<String, String> fieldTypes,
		Map<String, List<String>> enumValues,
		Map<String, Double> minValues,
		Map<String, Double> maxValues,
		List<String> nonEmptyPaths,
		Map<String, String> arrayItemTypes,
		boolean allowAdditionalFields,
		String examplePayload,
		String description
) {
	public ConfigDocumentSchema {
		requiredPaths = requiredPaths == null ? List.of() : List.copyOf(requiredPaths);
		fieldTypes = fieldTypes == null ? Map.of() : Map.copyOf(fieldTypes);
		enumValues = enumValues == null ? Map.of() : Map.copyOf(enumValues);
		minValues = minValues == null ? Map.of() : Map.copyOf(minValues);
		maxValues = maxValues == null ? Map.of() : Map.copyOf(maxValues);
		nonEmptyPaths = nonEmptyPaths == null ? List.of() : List.copyOf(nonEmptyPaths);
		arrayItemTypes = arrayItemTypes == null ? Map.of() : Map.copyOf(arrayItemTypes);
	}
}
