package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import org.bukkit.inventory.ItemStack;

public class PrerequisiteUsedItem implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	public PrerequisiteUsedItem(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		ItemStack usedItem = context.getUsedItem();
		return usedItem != null && mPrereqItem.prerequisiteMet(new ItemStack[] {usedItem});
	}
}
