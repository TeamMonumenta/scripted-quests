package com.playmonumenta.scriptedquests.utils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

public class InventoryUtils {
	public static boolean testForItemWithLore(ItemStack item, @Nullable String loreText, boolean exactMatch) {
		if (loreText == null || loreText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<Component> lore = meta.lore();
				if (lore != null && !lore.isEmpty()) {
					return lore.stream().anyMatch((entry) -> {
						String loreEntry = MessagingUtils.plainText(entry);
						return exactMatch ? loreEntry.equals(loreText) : loreEntry.contains(loreText);
					});
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
				Component displayComponent = meta.displayName();
				if (displayComponent != null) {
					String displayName = MessagingUtils.plainText(displayComponent);
					return exactMatch ? displayName.equals(nameText) : displayName.contains(nameText);
				}
			}
		}

		return false;
	}

	public static NamespacedKey getNamespacedKey(String path) throws Exception {
		return NamespacedKey.fromString(path);
	}

	public static LootTable getLootTable(String path) throws Exception {
		return getLootTable(getNamespacedKey(path));
	}

	public static @Nullable LootTable getLootTable(NamespacedKey lootNamespace) {
		return Bukkit.getLootTable(lootNamespace);
	}

	public static boolean giveLootTableContents(Player player, String lootPath, Random random, boolean alreadyDone) throws Exception {
		return giveLootTableContents(player, Objects.requireNonNull(getLootTable(lootPath)), random, alreadyDone);
	}

	public static boolean giveLootTableContents(Player player, LootTable lootTable, Random random, boolean alreadyDone) {
		LootContext lootContext = new LootContext.Builder(player.getLocation()).build();

		alreadyDone = giveItems(player, lootTable.populateLoot(random, lootContext), alreadyDone);
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
