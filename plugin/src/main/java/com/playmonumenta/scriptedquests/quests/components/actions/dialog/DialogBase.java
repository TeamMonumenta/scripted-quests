package com.playmonumenta.scriptedquests.quests.components.actions.dialog;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public interface DialogBase {
	/**
	 * Function should send the dialog to the player
	 *
	 * npcEntity is carried through so it can be used for prereq checks / actions later if needed
	 *
	 * npcEntity might be null (for all interactions except those involving an NPC)
	 */
	void sendDialog(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs);

	default JsonElement serialize(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		return JsonNull.INSTANCE;
	}
}
