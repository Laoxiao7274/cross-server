package org.xiaoziyi.crossserver.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.xiaoziyi.crossserver.auth.AuthService;

public final class AuthListener implements Listener {
	private final JavaPlugin plugin;
	private final AuthService authService;

	public AuthListener(JavaPlugin plugin, AuthService authService) {
		this.plugin = plugin;
		this.authService = authService;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		authService.initializePlayer(event.getPlayer());
		plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
			Player player = event.getPlayer();
			if (!player.isOnline() || authService.isAuthenticated(player.getUniqueId())) {
				task.cancel();
				return;
			}
			authService.tickPlayer(player);
		}, 20L, 20L);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onMove(PlayerMoveEvent event) {
		if (!authService.shouldBlock(event.getPlayer().getUniqueId())) {
			return;
		}
		if (event.getTo() != null && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
			event.setTo(event.getFrom());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		if (!authService.shouldBlock(event.getPlayer().getUniqueId())) {
			return;
		}
		if (!authService.isAllowedCommand(event.getMessage())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(authService.getBlockedCommandMessage());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChat(AsyncChatEvent event) {
		if (!authService.shouldBlock(event.getPlayer().getUniqueId())) {
			return;
		}
		event.setCancelled(true);
		authService.handleChatInput(event.getPlayer(), event.signedMessage().message());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (event.getPlayer() instanceof Player player && authService.shouldBlock(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player && authService.shouldBlock(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent event) {
		if (authService.shouldBlock(event.getPlayer().getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPickup(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player && authService.shouldBlock(player.getUniqueId())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (!authService.shouldBlock(event.getPlayer().getUniqueId())) {
			return;
		}
		Action action = event.getAction();
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		authService.clearPlayer(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onKick(PlayerKickEvent event) {
		authService.clearPlayer(event.getPlayer().getUniqueId());
	}
}
