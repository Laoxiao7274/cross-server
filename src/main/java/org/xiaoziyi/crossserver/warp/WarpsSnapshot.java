package org.xiaoziyi.crossserver.warp;

import java.util.Map;

public record WarpsSnapshot(
		Map<String, WarpEntry> warps
) {
}
