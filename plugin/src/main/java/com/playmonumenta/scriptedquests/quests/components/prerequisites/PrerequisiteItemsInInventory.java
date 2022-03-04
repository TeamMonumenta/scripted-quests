package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import org.bukkit.entity.Player;

public class PrerequisiteItemsInInventory implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	public PrerequisiteItemsInInventory(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		if (context.getEntityUsedForPrerequisites() instanceof Player player) {
			return mPrereqItem.prerequisiteMet(player.getInventory().getContents());
		}

		// Non-player entities can't test for items in their inventory
		return false;
	}
}
