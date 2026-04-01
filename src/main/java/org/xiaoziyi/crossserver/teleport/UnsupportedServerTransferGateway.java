package org.xiaoziyi.crossserver.teleport;

import org.bukkit.entity.Player;

public final class UnsupportedServerTransferGateway implements ServerTransferGateway {
	@Override
	public TeleportInitiationResult transfer(Player player, TeleportHandoff handoff) {
		return new TeleportInitiationResult(
				true,
				false,
				"§e跨服传送 handoff 已准备完成，但当前未接入真实切服网关。",
				handoff.requestId()
		);
	}
}
