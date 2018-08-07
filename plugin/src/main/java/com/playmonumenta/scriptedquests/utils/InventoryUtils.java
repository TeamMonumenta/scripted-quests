package com.playmonumenta.scriptedquests.utils;

import java.util.List;
import java.util.Random;

import net.minecraft.server.v1_12_R1.EntityItem;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.LootTable;
import net.minecraft.server.v1_12_R1.LootTableInfo;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.World;
import net.minecraft.server.v1_12_R1.WorldServer;

import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
				if (displayName != null && !displayName.isEmpty()) {
					if (displayName.contains(nameText)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static void giveLootTableContents(Player player, String lootPath) throws Exception{
		World nmsWorld = ((CraftWorld)player.getWorld()).getHandle();
		EntityPlayer nmsPlayer = ((CraftPlayer)player).getHandle();

		// Generate items from the specified loot table
		LootTable lootTable = nmsWorld.getLootTableRegistry().a(new MinecraftKey(lootPath));
		if (lootTable == null) {
			throw new Exception("Unable to find loot table '" + lootPath + "'");
		}

		List<net.minecraft.server.v1_12_R1.ItemStack> loot = lootTable.a(new Random(),
		                                                     new LootTableInfo(0, (WorldServer)nmsWorld,
		                                                             nmsWorld.getLootTableRegistry(),
		                                                             null, null, null));
		if (loot == null) {
			throw new Exception("Unable to retrieve loot from table '" + lootPath + "'");
		}

		// Give those items to the player (this code based on AdvancementRewards.java)
		for (net.minecraft.server.v1_12_R1.ItemStack itemstack : loot) {
			EntityItem entityitem = nmsPlayer.drop(itemstack, false);

			if (entityitem != null) {
				entityitem.r();
				entityitem.d(nmsPlayer.getName());
			}
		}
	}
}
