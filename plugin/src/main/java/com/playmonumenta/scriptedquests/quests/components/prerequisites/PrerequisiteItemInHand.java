package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PrerequisiteItemInHand implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	public PrerequisiteItemInHand(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		if (context.getEntityUsedForPrerequisites() instanceof Player player) {
			return mPrereqItem.prerequisiteMet(new ItemStack[] {player.getInventory().getItemInMainHand()});
		}

		// Non-player entities can't test for items in their inventory
		return false;
	}
}
