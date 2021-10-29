package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface ActionBase {
	/**
	 * Function should run the action, not checking the prereqs but passing them
	 * through to subsequent actions if applicable
	 *
	 * npcEntity is carried through so it can be used for prereq checks / actions later if needed
	 *
	 * npcEntity might be null (for all interactions except those involving an NPC)
	 */
	void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs);

	default JsonElement serializeForClientAPI(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		return JsonNull.INSTANCE;
	}
}
