package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PrerequisiteItemInEitherHand implements PrerequisiteBase {
	private final PrerequisiteItem mPrereqItem;

	public PrerequisiteItemInEitherHand(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			return mPrereqItem.prerequisiteMet(new ItemStack[] { ((Player)entity).getInventory().getItemInMainHand() }) ||
			       mPrereqItem.prerequisiteMet(new ItemStack[] { ((Player)entity).getInventory().getItemInOffHand() });
		}

		// Non-player entities can't test for items in their inventory
		return false;
	}
}
