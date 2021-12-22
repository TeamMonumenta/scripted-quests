package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.managers.InteractableManager;

public class PrerequisiteUsedItem implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	public PrerequisiteUsedItem(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		ItemStack usedItem = InteractableManager.getUsedItem();
		return usedItem != null && mPrereqItem.prerequisiteMet(new ItemStack[] {usedItem});
	}
}
