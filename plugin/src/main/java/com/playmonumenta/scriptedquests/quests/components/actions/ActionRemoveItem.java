package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ActionRemoveItem implements ActionBase {

	private final PrerequisiteItem mPrereqItem;

	public ActionRemoveItem(JsonElement element) throws Exception {
		mPrereqItem = new PrerequisiteItem(element);
	}

	@Override
	public void doActions(QuestContext context) {
		PlayerInventory inventory = context.getPlayer().getInventory();
		ItemStack[] contents = inventory.getContents();
		mPrereqItem.check(contents, true);
		inventory.setContents(contents);
	}

}
