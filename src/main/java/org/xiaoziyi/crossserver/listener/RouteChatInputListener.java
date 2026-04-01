package org.xiaoziyi.crossserver.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.xiaoziyi.crossserver.ui.RouteEditSessionService;

public final class RouteChatInputListener implements Listener {
	private final RouteEditSessionService routeEditSessionService;

	public RouteChatInputListener(RouteEditSessionService routeEditSessionService) {
		this.routeEditSessionService = routeEditSessionService;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChat(AsyncChatEvent event) {
		if (!routeEditSessionService.hasSession(event.getPlayer().getUniqueId())) {
			return;
		}
		event.setCancelled(true);
		routeEditSessionService.handleChat(event.getPlayer(), event.signedMessage().message());
	}
}
