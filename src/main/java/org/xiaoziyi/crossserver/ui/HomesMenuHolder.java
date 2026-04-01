package org.xiaoziyi.crossserver.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class HomesMenuHolder implements InventoryHolder {
	private final UUID playerId;
	private final int page;
	private Inventory inventory;

	public HomesMenuHolder(UUID playerId, int page) {
		this.playerId = playerId;
		this.page = page;
	}

	public UUID playerId() {
		return playerId;
	}

	public int page() {
		return page;
	}

	public void bind(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
