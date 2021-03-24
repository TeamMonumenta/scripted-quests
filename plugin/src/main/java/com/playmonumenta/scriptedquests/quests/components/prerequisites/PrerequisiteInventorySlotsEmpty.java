package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;

public class PrerequisiteInventorySlotsEmpty implements PrerequisiteBase {

	private int mSlotsNeeded;

	public PrerequisiteInventorySlotsEmpty(JsonElement value) throws Exception {
		try {
			mSlotsNeeded = Integer.parseInt(value.toString());
		} catch (NumberFormatException e) {
			throw new Exception("Error: " + value.toString() + " is not an integer!");
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Inventory inventory = ((Player)entity).getInventory();
			int emptySlots = 0;
			for (ItemStack item : inventory) {
				if (item == null || item.getType() == Material.AIR) {
					emptySlots++;
				}
			}

			return emptySlots >= mSlotsNeeded;
		}

		// Non-Player entities do not have inventories
		return false;
	}
}
