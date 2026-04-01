package org.xiaoziyi.crossserver.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WarpMenuHolder implements InventoryHolder {
	private final UUID playerId;
	private final int page;
	private final Map<Integer, String> slotWarps = new HashMap<>();
	private Inventory inventory;

	public WarpMenuHolder(UUID playerId, int page) {
		this.playerId = playerId;
		this.page = page;
	}

	public UUID playerId() {
		return playerId;
	}

	public int page() {
		return page;
	}

	public void bindWarp(int slot, String warpName) {
		slotWarps.put(slot, warpName);
	}

	public String warpAt(int slot) {
		return slotWarps.get(slot);
	}

	public void bind(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}
}
