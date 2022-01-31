package com.playmonumenta.scriptedquests.quests.components.prerequisites.quests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.QuestData;
import com.playmonumenta.scriptedquests.quests.components.prerequisites.PrerequisiteBase;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PrerequisiteIsOnQuestStage implements PrerequisiteBase {

	private final String mQuestId;
	private final int mStage;
	public PrerequisiteIsOnQuestStage(JsonElement value) throws Exception {
		JsonObject object = value.getAsJsonObject();
		if (object == null) {
			throw new Exception("is on quest stage value is not a object!");
		}
		mQuestId = object.get("quest_id").getAsString();
		mStage = object.get("quest_stage").getAsInt();
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player player) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			QuestData questData = data.getQuestData(mQuestId);
			if (questData != null) {
				return questData.getStage() == mStage;
			}
		}
		return false;
	}

}
