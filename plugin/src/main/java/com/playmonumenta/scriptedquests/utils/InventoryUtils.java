package com.playmonumenta.scriptedquests.utils;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.loot.LootContext;
import org.bukkit.NamespacedKey;

public class InventoryUtils {
	public static boolean testForItemWithLore(ItemStack item, String loreText) {
		if (loreText == null || loreText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (loreEntry.contains(loreText)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	// TODO: This will *not* match items that don't have an NBT name (stick, stone sword, etc.)
	public static boolean testForItemWithName(ItemStack item, String nameText) {
		if (nameText == null || nameText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				String displayName = meta.getDisplayName();
				if (displayName != null && !displayName.isEmpty() && displayName.contains(nameText)) {
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

	public static void giveLootTableContents(Player player, String lootPath, Random random) throws Exception {
		NamespacedKey lootNamespace = getNamespacedKey(lootPath);
		LootContext lootContext = new LootContext.Builder(player.getLocation()).build();

		PlayerInventory inv = player.getInventory();
		for (ItemStack item : Bukkit.getLootTable(lootNamespace).populateLoot(random, lootContext)) {
			inv.addItem(item);
		}
	}
}
