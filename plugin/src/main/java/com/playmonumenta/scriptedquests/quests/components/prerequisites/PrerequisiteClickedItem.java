package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.managers.InteractableManager;

public class PrerequisiteClickedItem implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	public PrerequisiteClickedItem(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		ItemStack clickedItem = InteractableManager.getClickedItem();
		return clickedItem != null && mPrereqItem.prerequisiteMet(new ItemStack[] {clickedItem});
	}
}
