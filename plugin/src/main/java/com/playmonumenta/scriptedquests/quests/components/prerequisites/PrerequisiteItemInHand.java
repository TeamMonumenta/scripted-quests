package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonElement;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

class PrerequisiteItemInHand implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	PrerequisiteItemInHand(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		return mPrereqItem.prerequisiteMet(new ItemStack[] { player.getInventory().getItemInMainHand() });
	}
}
