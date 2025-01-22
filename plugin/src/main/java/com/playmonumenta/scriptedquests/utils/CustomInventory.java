package com.playmonumenta.scriptedquests.utils;

import java.util.HashSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This code was originally copied from NBTEditor, Copyright (C) 2013-2018 Gonçalo Baltazar (me@goncalomb.com), released under GPLv3.
 * <p>
 * Original source: https://github.com/goncalomb/NBTEditor/blob/master/src/main/java/com/goncalomb/bukkit/mylib/utils/CustomInventory.java
 * <p>
 * Note: upstream implementation sucks, needs to be rewritten for to be even slightly functional.
 * TODO: this absolutely sucks, but I don't want to break ABI
 */
public abstract class CustomInventory implements InventoryHolder {
	private @Nullable Plugin mOwner = null;
	protected final Inventory mInventory;

	public CustomInventory(@SuppressWarnings("PMD.UnusedFormalParameter") /* abi */ Player owner, int size) {
		mInventory = Bukkit.createInventory(this, size);
	}

	public CustomInventory(@SuppressWarnings("PMD.UnusedFormalParameter") /* abi */Player owner, int size, String title) {
		mInventory = Bukkit.createInventory(this, size, Component.text(title));
	}

	public CustomInventory(@SuppressWarnings("PMD.UnusedFormalParameter") /* abi */Player owner, int size, Component title) {
		mInventory = Bukkit.createInventory(this, size, title);
	}

	public final void openInventory(Player player, Plugin owner) {
		if (mOwner == null) {
			player.openInventory(mInventory);
			this.mOwner = owner;
		}
	}

	@Override
	public @NotNull Inventory getInventory() {
		return mInventory;
	}

	public @Nullable Plugin getPlugin() {
		return mOwner;
	}

	public final void close() {
		if (mOwner != null) {
			for (HumanEntity human : mInventory.getViewers()) {
				human.closeInventory();
			}
		}
	}

	protected void inventoryDrag(InventoryDragEvent event) {
		event.setCancelled(true);
	}

	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		InventoryUtils.refreshOffhand(event);
	}

	protected void inventoryClose(InventoryCloseEvent event) {
	}

	/**
	 * Here because I'm not willing to change the access of inventory[event] handlers (ABI)
	 */
	public static class InventoryListener implements Listener {
		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void inventoryClick(InventoryClickEvent event) {
			if (event.getInventory().getHolder(false) instanceof CustomInventory inv) {
				inv.inventoryClick(event);
				InventoryUtils.refreshOffhand(event);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void inventoryDrag(InventoryDragEvent event) {
			if (event.getInventory().getHolder(false) instanceof CustomInventory inv) {
				inv.inventoryDrag(event);
			}
		}

		@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
		public void inventoryClose(InventoryCloseEvent event) {
			if (event.getInventory().getHolder(false) instanceof CustomInventory inv) {
				inv.inventoryClose(event);
			}
		}
	}
}
