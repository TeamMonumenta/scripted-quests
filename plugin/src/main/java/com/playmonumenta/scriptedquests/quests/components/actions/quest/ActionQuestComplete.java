package com.playmonumenta.scriptedquests.quests.components.actions.quest;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionNested;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionQuestComplete extends ActionQuest implements ActionBase {

	private ActionNested mParent;
	public ActionQuestComplete(JsonElement value, ActionNested parent) {
		mParent = parent;

		// Get all combined prerequisites to get to this action.
		mPrerequisites = mergePrerequisites(parent);
		addToTopParent(parent);
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {

	}

}
