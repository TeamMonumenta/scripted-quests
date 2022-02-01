package com.playmonumenta.scriptedquests.quests.components.prerequisites.quests;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.Quest;
import com.playmonumenta.scriptedquests.quests.QuestData;
import com.playmonumenta.scriptedquests.quests.QuestLine;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteBase;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PrerequisiteIsQuestLineCompleted implements PrerequisiteBase {

	private final String mQuestlineId;
	public PrerequisiteIsQuestLineCompleted(JsonElement value) throws Exception {
		mQuestlineId = value.getAsString();
		if (mQuestlineId == null) {
			throw new Exception("is_questline_completed value is not a string!");
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player player) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			QuestLine questLine = Plugin.getInstance().mQuestManager.getQuestline(mQuestlineId);
			if (questLine != null) {
				for (QuestLine.QuestGroup questGroup : questLine.getQuestGroups()) {
					for (String key : questGroup.getQuests()) {
						Quest quest = Plugin.getInstance().mQuestManager.getQuest(key); // Let's check if this quest still exists. If it doesn't skip over it.
						if (quest != null) {
							QuestData questData = data.getQuestData(quest.getQuestId());
							if (questData == null || !questData.mCompleted) {
								return false;
							}
						}
					}
				}

				return true;
			}
		}
		return false;
	}

}
