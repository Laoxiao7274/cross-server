package org.xiaoziyi.crossserver.web;

import java.time.Instant;

public record WebPanelLogEntry(
		Instant occurredAt,
		String level,
		String message
) {
}
