package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
	public boolean prerequisiteMet(QuestContext context) {
		if (context.getEntityUsedForPrerequisites() instanceof Player player) {
			PlayerInventory inventory = player.getInventory();
			int emptySlots = 0;
			for (int i = 0; i < 36; i++) {
				ItemStack item = inventory.getItem(i);
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
