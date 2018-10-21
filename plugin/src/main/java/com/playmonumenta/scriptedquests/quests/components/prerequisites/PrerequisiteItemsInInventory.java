package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonElement;

import org.bukkit.entity.Player;

class PrerequisiteItemsInInventory implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	PrerequisiteItemsInInventory(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		return mPrereqItem.prerequisiteMet(player.getInventory().getContents());
	}
}
