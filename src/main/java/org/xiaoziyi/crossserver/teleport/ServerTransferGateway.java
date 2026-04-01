package org.xiaoziyi.crossserver.teleport;

import org.bukkit.entity.Player;

public interface ServerTransferGateway {
	TeleportInitiationResult transfer(Player player, TeleportHandoff handoff);
}
