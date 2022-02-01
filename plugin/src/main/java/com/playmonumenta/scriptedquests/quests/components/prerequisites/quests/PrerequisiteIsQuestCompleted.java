package com.playmonumenta.scriptedquests.quests.components.prerequisites.quests;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.Quest;
import com.playmonumenta.scriptedquests.quests.QuestData;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteBase;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PrerequisiteIsQuestCompleted implements PrerequisiteBase {

	private final String mQuestId;
	public PrerequisiteIsQuestCompleted(JsonElement value) throws Exception {
		mQuestId = value.getAsString();
		if (mQuestId == null) {
			throw new Exception("can accept quest value is not a string!");
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player player) {
			Quest quest = Plugin.getInstance().mQuestManager.getQuest(mQuestId);
			if (quest != null) {
				PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
				QuestData questData = data.getQuestData(mQuestId);
				return questData != null && questData.mCompleted;
			}
		}
		return false;
	}

}
