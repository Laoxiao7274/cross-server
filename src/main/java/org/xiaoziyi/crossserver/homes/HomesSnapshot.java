package org.xiaoziyi.crossserver.homes;

import java.util.LinkedHashMap;
import java.util.Map;

public record HomesSnapshot(
		String defaultHome,
		Map<String, HomeEntry> homes
) {
	public HomesSnapshot {
		homes = homes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(homes);
	}
}
