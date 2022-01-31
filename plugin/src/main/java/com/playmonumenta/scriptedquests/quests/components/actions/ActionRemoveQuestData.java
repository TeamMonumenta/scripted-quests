package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestData;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionRemoveQuestData implements ActionBase {

	private String mQuestData;

	public ActionRemoveQuestData(JsonElement value) throws Exception {
		mQuestData = value.getAsString();
		if (mQuestData == null) {
			throw new Exception("questdata value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());

		QuestData questData = data.getQuestData(mQuestData);
		if (questData != null) {
			data.getQuestDataList().remove(questData);
			data.updateQuestVisibility();
			if (data.mScoreboard != null) {
				data.mScoreboard.updateScoreboard();
			}
		}
	}

}
