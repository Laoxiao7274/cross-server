package org.xiaoziyi.crossserver.auth;

import java.time.Instant;

public record AuthProfile(
		String passwordHash,
		String salt,
		Instant registeredAt,
		Instant lastLoginAt,
		String lastLoginServer,
		long loginTicketVersion
) {
}
