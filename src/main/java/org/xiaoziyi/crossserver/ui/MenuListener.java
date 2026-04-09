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
	private final CrossServerMainMenuService crossServerMainMenuService;

	public MenuListener(HomesMenuService homesMenuService, WarpMenuService warpMenuService, TransferAdminMenuService transferAdminMenuService, RouteConfigMenuService routeConfigMenuService, CrossServerMainMenuService crossServerMainMenuService) {
		this.homesMenuService = homesMenuService;
		this.warpMenuService = warpMenuService;
		this.transferAdminMenuService = transferAdminMenuService;
		this.routeConfigMenuService = routeConfigMenuService;
		this.crossServerMainMenuService = crossServerMainMenuService;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof HomesMenuHolder holder) {
			event.setCancelled(true);
			if (homesMenuService != null && event.getClickedInventory() == event.getView().getTopInventory()) {
				homesMenuService.handleClick(player, holder, event.getSlot(), event.isLeftClick(), event.isRightClick(), event.isShiftClick(), event.getClick() == ClickType.MIDDLE);
			}
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof WarpMenuHolder holder) {
			event.setCancelled(true);
			if (warpMenuService != null && event.getClickedInventory() == event.getView().getTopInventory()) {
				warpMenuService.handleClick(player, holder, event.getSlot(), event.isLeftClick(), event.isRightClick(), event.isShiftClick());
			}
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof TransferAdminMenuHolder holder) {
			event.setCancelled(true);
			if (transferAdminMenuService != null && event.getClickedInventory() == event.getView().getTopInventory()) {
				transferAdminMenuService.handleClick(player, holder, event.getSlot());
			}
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof RouteConfigMenuHolder holder) {
			event.setCancelled(true);
			if (routeConfigMenuService != null && event.getClickedInventory() == event.getView().getTopInventory()) {
				routeConfigMenuService.handleClick(player, holder, event.getSlot(), event.isLeftClick(), event.isRightClick());
			}
			return;
		}
		if (event.getView().getTopInventory().getHolder() instanceof CrossServerMainMenuHolder) {
			event.setCancelled(true);
			if (crossServerMainMenuService != null && event.getClickedInventory() == event.getView().getTopInventory()) {
				crossServerMainMenuService.handleClick(player, event.getSlot());
			}
		}
	}
}
