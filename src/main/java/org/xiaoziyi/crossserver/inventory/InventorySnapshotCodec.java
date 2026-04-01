package org.xiaoziyi.crossserver.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class InventorySnapshotCodec {
	private InventorySnapshotCodec() {
	}

	public static String encode(ItemStack[] contents) {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			try (BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(byteStream)) {
				outputStream.writeInt(contents.length);
				for (ItemStack item : contents) {
					outputStream.writeObject(item);
				}
			}
			return Base64.getEncoder().encodeToString(byteStream.toByteArray());
		} catch (IOException exception) {
			throw new IllegalStateException("无法序列化背包数据", exception);
		}
	}

	public static ItemStack[] decode(String payload) {
		try {
			byte[] bytes = Base64.getDecoder().decode(payload);
			try (BukkitObjectInputStream inputStream = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
				int size = inputStream.readInt();
				ItemStack[] contents = new ItemStack[size];
				for (int i = 0; i < size; i++) {
					contents[i] = (ItemStack) inputStream.readObject();
				}
				return contents;
			}
		} catch (IOException | ClassNotFoundException exception) {
			throw new IllegalStateException("无法反序列化背包数据", exception);
		}
	}

	public static void apply(Inventory inventory, String payload) {
		ItemStack[] contents = decode(payload);
		if (inventory.getSize() != contents.length) {
			ItemStack[] resized = new ItemStack[inventory.getSize()];
			System.arraycopy(contents, 0, resized, 0, Math.min(contents.length, resized.length));
			inventory.setContents(resized);
			return;
		}
		inventory.setContents(contents);
	}
}
