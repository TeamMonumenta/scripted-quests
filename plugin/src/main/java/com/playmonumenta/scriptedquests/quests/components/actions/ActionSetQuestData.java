package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.quests.QuestData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionSetQuestData implements ActionBase {

	private class SetField {
		// This should be an enum, but idk how to set those up. -Nick
		private int mOperation;
		private static final int SET_EXACT = 1;
		private static final int INCREMENT = 2;

		String mOtherScore;
		int mMin;
		int mMax;

		int value;
		String mFieldName;

		SetField(String fieldName, String parameter) throws Exception {
			mFieldName = fieldName;
			mOperation = SET_EXACT;
			if (parameter.startsWith("+") || parameter.startsWith("-")) {
				mOperation = INCREMENT;
				parameter = parameter.substring(1);
			}

			value = Integer.parseInt(parameter);
		}

		boolean set(Player player, String questId) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			QuestData questData = data.getQuestData(questId);

			if (questData == null) {
				questData = new QuestData(questId);
				data.getQuestDataList().add(questData);
			}
			
			return false;
		}

	}

	/*
	 *    {
      		"quest_id": "",
      		"quest_data_fields": {
   				"zombie_kills": "+1"
  			}
   		  }
	 */

	private String mId;
	public ActionSetQuestData(JsonElement value) throws Exception {
		JsonObject object = value.getAsJsonObject();
		if (object == null) {
			throw new Exception("quest data value is not an object!");
		}

		mId = object.get("quest_id").getAsString();
		if (mId == null) {
			throw new Exception("quest_id value is not a string!");
		}



	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {

	}
}
