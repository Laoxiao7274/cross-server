package org.xiaoziyi.crossserver.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class MenuListener implements Listener {
	private final HomesMenuService homesMenuService;
	private final WarpMenuService warpMenuService;
	private final TransferAdminMenuService transferAdminMenuService;
	private final RouteConfigMenuService routeConfigMenuService;

	public MenuListener(HomesMenuService homesMenuService, WarpMenuService warpMenuService, TransferAdminMenuService transferAdminMenuService, RouteConfigMenuService routeConfigMenuService) {
		this.homesMenuService = homesMenuService;
		this.warpMenuService = warpMenuService;
		this.transferAdminMenuService = transferAdminMenuService;
		this.routeConfigMenuService = routeConfigMenuService;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof HomesMenuHolder holder) {
			event.setCancelled(true);
			if (event.getClickedInventory() == event.getView().getTopInventory()) {
				homesMenuService.handleClick(player, holder, event.getSlot(), event.isLeftClick(), event.isRightClick(), event.isShiftClick(), event.getClick() == ClickType.MIDDLE);
			}
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof WarpMenuHolder holder) {
			event.setCancelled(true);
			if (event.getClickedInventory() == event.getView().getTopInventory()) {
				warpMenuService.handleClick(player, holder, event.getSlot(), event.isLeftClick(), event.isRightClick(), event.isShiftClick());
			}
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof TransferAdminMenuHolder holder) {
			event.setCancelled(true);
			if (event.getClickedInventory() == event.getView().getTopInventory()) {
				transferAdminMenuService.handleClick(player, holder, event.getSlot());
			}
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof RouteConfigMenuHolder holder) {
			event.setCancelled(true);
			if (event.getClickedInventory() == event.getView().getTopInventory()) {
				routeConfigMenuService.handleClick(player, holder, event.getSlot(), event.isLeftClick(), event.isRightClick());
			}
		}
	}
}
