package com.playmonumenta.scriptedquests.utils;

import java.util.HashMap;
import java.util.HashSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * This code was originally copied from NBTEditor, Copyright (C) 2013-2018 Gonçalo Baltazar (me@goncalomb.com), released under GPLv3.
 * <p>
 * Original source: https://github.com/goncalomb/NBTEditor/blob/master/src/main/java/com/goncalomb/bukkit/mylib/utils/CustomInventory.java
 */
public abstract class CustomInventory {

	private static @Nullable Listener mMainListener;
	private static @Nullable Plugin mPlugin;
	private static final HashMap<HumanEntity, CustomInventory> mOpenedInvsByPlayer = new HashMap<>();
	private static final HashMap<Plugin, HashSet<CustomInventory>> mOpenedInvsByPlugin = new HashMap<>();

	private @Nullable Plugin mOwner = null;
	protected final Inventory mInventory;

	private static void bindListener(Plugin plugin) {
		if (mPlugin == null) {
			if (mMainListener == null) {
				mMainListener = new Listener() {

					@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
					public void pluginDisable(PluginDisableEvent event) {
						HashSet<CustomInventory> invs = mOpenedInvsByPlugin.remove(event.getPlugin());
						if (invs != null) {
							for (CustomInventory inv : invs) {
								for (HumanEntity human : inv.mInventory.getViewers().toArray(new HumanEntity[0])) {
									mOpenedInvsByPlayer.remove(human);
									human.closeInventory();
								}
							}
						}

						if (mPlugin == event.getPlugin()) {
							mPlugin = null;
							HandlerList.unregisterAll(mMainListener);
							if (mOpenedInvsByPlugin.size() > 0) {
								bindListener(mOpenedInvsByPlugin.keySet().iterator().next());
							}
						}
					}

					@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
					public void inventoryClick(InventoryClickEvent event) {
						CustomInventory inv = mOpenedInvsByPlayer.get(event.getWhoClicked());
						if (inv != null) {
							inv.inventoryClick(event);
							InventoryUtils.refreshOffhand(event);
						}
					}

					@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
					public void inventoryDrag(InventoryDragEvent event) {
						CustomInventory inv = mOpenedInvsByPlayer.get(event.getWhoClicked());
						if (inv != null) {
							inv.inventoryDrag(event);
						}
					}

					@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
					public void inventoryClose(InventoryCloseEvent event) {
						CustomInventory inv = mOpenedInvsByPlayer.remove(event.getPlayer());
						if (inv != null) {
							HashSet<CustomInventory> owner = mOpenedInvsByPlugin.get(inv.mOwner);
							if (owner != null) {
								owner.remove(inv);
							}
							inv.inventoryClose(event);
						}
					}

				};
			}

			Bukkit.getPluginManager().registerEvents(mMainListener, plugin);
			mPlugin = plugin;
		}
	}

	public CustomInventory(Player owner, int size) {
		mInventory = Bukkit.createInventory(owner, size);
	}

	public CustomInventory(Player owner, int size, String title) {
		mInventory = Bukkit.createInventory(owner, size, Component.text(title));
	}

	public CustomInventory(Player owner, int size, Component title) {
		mInventory = Bukkit.createInventory(owner, size, title);
	}

	public void openInventory(Player player, Plugin owner) {
		if (mOwner == null) {
			player.openInventory(mInventory);
			mOpenedInvsByPlayer.put(player, this);

			this.mOwner = owner;

			mOpenedInvsByPlugin.computeIfAbsent(owner, k -> new HashSet<>()).add(this);

			bindListener(owner);
		}
	}

	public Inventory getInventory() {
		return mInventory;
	}

	public @Nullable Plugin getPlugin() {
		return mOwner;
	}

	public final void close() {
		if (mOwner != null) {
			for (HumanEntity human : mInventory.getViewers().toArray(new HumanEntity[0])) {
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

}
