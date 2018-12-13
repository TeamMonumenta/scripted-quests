package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;

import org.bukkit.entity.Player;

public class PrerequisiteItemsInInventory implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	public PrerequisiteItemsInInventory(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		return mPrereqItem.prerequisiteMet(player.getInventory().getContents());
	}
}
