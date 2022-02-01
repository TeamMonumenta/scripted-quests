package com.playmonumenta.scriptedquests.quests.components.prerequisites.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.Quest;
import com.playmonumenta.scriptedquests.quests.QuestData;
import com.playmonumenta.scriptedquests.quests.QuestLine;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteBase;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PrerequisiteCanAcceptQuest implements PrerequisiteBase {

	private final String mQuestId;
	public PrerequisiteCanAcceptQuest(JsonElement value) throws Exception {
		mQuestId = value.getAsString();
		if (mQuestId == null) {
			throw new Exception("can accept quest value is not a string!");
		}
	}

	public PrerequisiteCanAcceptQuest(String questId) {
		mQuestId = questId;
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player player) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			QuestData questData = data.getQuestData(mQuestId);

			if (questData == null) {
				Quest quest = Plugin.getInstance().mQuestManager.getQuest(mQuestId);
				if (quest != null && data.getLevel() >= quest.getLevel()
				&& (quest.getPrerequisites() == null || quest.getPrerequisites().prerequisiteMet(player, npcEntity))) {

					QuestLine.QuestGroup group = quest.getQuestlineGroup();
					if (group != null) {
						int order = group.getOrderInLine();
						QuestLine questLine = group.getQuestline();
						for (int i = 0; i < order; i++) {
							QuestLine.QuestGroup groupCheck = questLine.getQuestGroups().get(i);

							for (String quest_id : groupCheck.getQuests()) {
								QuestData checkData = data.getQuestData(quest_id);
								if (checkData == null || !checkData.mCompleted) {
									return false;
								}
							}
						}

					}
					return true;
				}
			}

		}
		return false;
	}
}
