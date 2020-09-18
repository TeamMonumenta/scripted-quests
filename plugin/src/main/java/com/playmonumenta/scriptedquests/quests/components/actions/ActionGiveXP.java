package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionGiveXP implements ActionBase {

	private Integer mXp;
	public ActionGiveXP(JsonElement value) throws Exception {
		mXp = value.getAsInt();

		if (mXp == null) {
			throw new Exception("XP value is not an integer!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
		data.giveXP(mXp);
	}
}
