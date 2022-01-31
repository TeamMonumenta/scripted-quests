package com.playmonumenta.scriptedquests.utils;

import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class InventoryUtils {
	public static boolean testForItemWithLore(ItemStack item, @Nullable String loreText, boolean exactMatch) {
		if (loreText == null || loreText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (exactMatch ? ChatColor.stripColor(loreEntry).equals(loreText) : loreEntry.contains(loreText)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	// TODO: This will *not* match items that don't have an NBT name (stick, stone sword, etc.)
	public static boolean testForItemWithName(ItemStack item, @Nullable String nameText, boolean exactMatch) {
		if (nameText == null || nameText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				String displayName = meta.getDisplayName();
				if (displayName != null
					    && !displayName.isEmpty()
					    && (exactMatch ? ChatColor.stripColor(displayName).equals(nameText) : displayName.contains(nameText))) {
					return true;
				}
			}
		}

		return false;
	}

	public static NamespacedKey getNamespacedKey(String path) throws Exception {
		String[] str = path.split(":", 2);
		if (str[1].contains(":")) {
			throw new Exception("Path '" + path + "' is not a valid minecraft namespace!");
		}
		return new NamespacedKey(str[0], str[1]);
	}

	public static boolean giveLootTableContents(Player player, String lootPath, Random random, boolean alreadyDone) throws Exception {
		NamespacedKey lootNamespace = getNamespacedKey(lootPath);
		LootContext lootContext = new LootContext.Builder(player.getLocation()).build();

		alreadyDone = giveItems(player, Bukkit.getLootTable(lootNamespace).populateLoot(random, lootContext), alreadyDone);
		return alreadyDone;
	}

	public static boolean giveItems(Player player, Collection<ItemStack> items, boolean alreadyDone) {
		PlayerInventory inv = player.getInventory();
		boolean itemsDropped = false;
		for (ItemStack item : items) {
			if (inv.firstEmpty() == -1) {
				itemsDropped = true;
				Location ploc = player.getLocation();
				ploc.getWorld().dropItem(ploc, item);
			} else {
				inv.addItem(item);
			}
		}

		if (itemsDropped && !alreadyDone) {
			player.sendMessage(ChatColor.RED + "Your inventory is full! Some items were dropped on the ground!");
			alreadyDone = true;
		}
		return alreadyDone;
	}
}
