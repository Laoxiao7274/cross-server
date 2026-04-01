package org.xiaoziyi.crossserver.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public final class RouteConfigMenuHolder implements InventoryHolder {
	private final int page;
	private final Map<Integer, String> slotTargets = new HashMap<>();
	private Inventory inventory;

	public RouteConfigMenuHolder(int page) {
		this.page = page;
	}

	public int page() {
		return page;
	}

	public void bindTarget(int slot, String serverId) {
		slotTargets.put(slot, serverId);
	}

	public String targetAt(int slot) {
		return slotTargets.get(slot);
	}

	public void bind(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
