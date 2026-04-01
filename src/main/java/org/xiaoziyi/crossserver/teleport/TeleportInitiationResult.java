package org.xiaoziyi.crossserver.teleport;

public record TeleportInitiationResult(
		boolean success,
		boolean gatewayReady,
		String message,
		String requestId
) {
}
