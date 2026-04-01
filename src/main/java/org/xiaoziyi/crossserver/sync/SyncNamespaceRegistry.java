package org.xiaoziyi.crossserver.sync;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SyncNamespaceRegistry {
	private final Set<String> namespaces = ConcurrentHashMap.newKeySet();

	public void registerNamespace(String namespace) {
		namespaces.add(namespace);
	}

	public boolean isRegistered(String namespace) {
		return namespaces.contains(namespace);
	}

	public Set<String> getNamespaces() {
		return Collections.unmodifiableSet(namespaces);
	}
}
