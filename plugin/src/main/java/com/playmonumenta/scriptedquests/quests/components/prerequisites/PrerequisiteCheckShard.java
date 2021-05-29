package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.shards.ShardInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PrerequisiteCheckShard implements PrerequisiteBase{

	private boolean mInverted;
	private String mShard;

	public PrerequisiteCheckShard(JsonElement value) throws Exception {
		String tag = value.getAsString();
		if (tag == null) {
			throw new Exception("class value is not a string!");
		}

		if (tag.charAt(0) == '!') {
			mInverted = true;
			mShard = tag.substring(1);
		} else {
			mInverted = false;
			mShard = tag;
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			ShardInstance instance = Core.getInstance().mShardManager.getShard(player);
			if (instance != null) {
				return instance.mShard.mId.equalsIgnoreCase(mShard);
			}
		}
		return false;
	}
}
