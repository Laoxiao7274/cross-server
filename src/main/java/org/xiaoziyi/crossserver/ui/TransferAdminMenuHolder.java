package org.xiaoziyi.crossserver.ui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TransferAdminMenuHolder implements InventoryHolder {
	public enum ViewMode {
		RECENT,
		DETAIL,
		HISTORY
	}

	private final UUID targetPlayerId;
	private final ViewMode viewMode;
	private final int page;
	private final int parentPage;
	private final Map<Integer, UUID> slotTargets = new HashMap<>();
	private Inventory inventory;

	public TransferAdminMenuHolder(UUID targetPlayerId) {
		this(targetPlayerId, ViewMode.DETAIL, 1, 1);
	}

	public TransferAdminMenuHolder(UUID targetPlayerId, ViewMode viewMode, int page, int parentPage) {
		this.targetPlayerId = targetPlayerId;
		this.viewMode = viewMode;
		this.page = page;
		this.parentPage = parentPage;
	}

	public UUID targetPlayerId() {
		return targetPlayerId;
	}

	public ViewMode viewMode() {
		return viewMode;
	}

	public boolean recentView() {
		return viewMode == ViewMode.RECENT;
	}

	public boolean detailView() {
		return viewMode == ViewMode.DETAIL;
	}

	public boolean historyView() {
		return viewMode == ViewMode.HISTORY;
	}

	public int page() {
		return page;
	}

	public int parentPage() {
		return parentPage;
	}

	public void bindTarget(int slot, UUID playerId) {
		slotTargets.put(slot, playerId);
	}

	public UUID targetAt(int slot) {
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
