package org.xiaoziyi.crossserver.teleport;

import java.util.List;

public record TransferPageResult(
		int page,
		int pageSize,
		int totalItems,
		int totalPages,
		List<TransferHistoryEntry> entries
) {
}
