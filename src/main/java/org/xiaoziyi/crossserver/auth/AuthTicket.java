package org.xiaoziyi.crossserver.auth;

import java.time.Instant;

public record AuthTicket(
		String tokenId,
		Instant issuedAt,
		Instant expiresAt,
		String issuedServer,
		long version
) {
}
