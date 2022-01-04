package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.music.RPGSong;
import me.Novalescent.shards.ShardInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ActionSetShardMusic implements ActionBase {

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
				shardInstance.changeObjective(mFieldName, mParameter);
				return true;
			}
			return false;
		}

	}

	private final List<SetField> mFields = new ArrayList<>();
	private RPGSong mSong = null;
	private boolean mSilence = false;

	public ActionSetShardMusic(JsonElement element) throws Exception {
		if (!element.isJsonPrimitive()) {
			throw new Exception("Set Shard Music is not a String!");
		}

		String value = element.getAsString();
		if (value.isEmpty()) {
			mSilence  = true;
		} else {
			mSong = Core.getInstance().mMusicManager.getSong(value);
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		ShardInstance shardInstance = Core.getInstance().mShardManager.getShard(player);
		if (mSong != null || mSilence) {
			shardInstance.setMusic(mSong);
		}
	}
}
