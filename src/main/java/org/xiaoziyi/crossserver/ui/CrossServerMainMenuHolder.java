package org.xiaoziyi.crossserver.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class CrossServerMainMenuHolder implements InventoryHolder {
	private Inventory inventory;

	public void bind(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
