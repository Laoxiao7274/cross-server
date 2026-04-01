package org.xiaoziyi.crossserver.teleport;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportArrivalListener implements Listener {
	private final JavaPlugin plugin;
	private final CrossServerTeleportService teleportService;
	private final long arrivalCheckDelayTicks;

	public TeleportArrivalListener(JavaPlugin plugin, CrossServerTeleportService teleportService, long arrivalCheckDelayTicks) {
		this.plugin = plugin;
		this.teleportService = teleportService;
		this.arrivalCheckDelayTicks = arrivalCheckDelayTicks;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> teleportService.tryConsumeArrival(event.getPlayer()), arrivalCheckDelayTicks);
	}
}
