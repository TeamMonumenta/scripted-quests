package com.playmonumenta.scriptedquests.quests.components.actions.shard;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import me.Novalescent.Core;
import me.Novalescent.shards.ShardInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionTriggerShardEvent implements ActionBase {

	private final String mEventId;
	public ActionTriggerShardEvent(JsonElement element) throws Exception {
		mEventId = element.getAsString();
		if (mEventId == null) {
			throw new Exception("Shard value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		ShardInstance shardInstance = Core.getInstance().mShardManager.getShard(player);
		if (shardInstance != null) {
			shardInstance.triggerEvent(mEventId);
		}
	}
}
