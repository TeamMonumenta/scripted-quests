package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionGiveReward implements ActionBase {


	public ActionGiveReward(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();

		if (object == null) {
			throw new Exception("give_reward value is not an object!");
		}

	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {

	}
}
