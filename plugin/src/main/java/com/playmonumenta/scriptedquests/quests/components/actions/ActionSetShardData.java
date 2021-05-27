package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestDataLink;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.options.RPGOption;
import me.Novalescent.player.quests.QuestData;
import me.Novalescent.player.quests.QuestTemplate;
import me.Novalescent.player.scoreboards.PlayerScoreboard;
import me.Novalescent.shards.ShardInstance;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ActionSetShardData implements ActionBase {

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
		String mParameter;

		SetField(String fieldName, String parameter) throws Exception {
			mFieldName = fieldName;
			mParameter = parameter;
		}

		boolean set(Entity entity) {
			ShardInstance shardInstance = Core.getInstance().mShardManager.getShard(entity);

			if (shardInstance != null) {
				shardInstance.changeField(mFieldName, mParameter);
				return true;
			}
			return false;
		}

	}

	private List<SetField> mFields = new ArrayList<>();

	public ActionSetShardData(JsonElement element) throws Exception {
		if (!element.isJsonArray()) {
			throw new Exception("Set Shard Data is not an instance of an array!");
		}

		JsonArray array = element.getAsJsonArray();

		for (JsonElement arrayElement : array) {
			JsonObject jsonObject = arrayElement.getAsJsonObject();
			SetField field = new SetField(jsonObject.get("shard_field_id").getAsString(), jsonObject.get("shard_field_data").getAsString());
			mFields.add(field);
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		for (SetField setField : mFields) {
			setField.set(player);
		}
	}
}
