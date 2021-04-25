package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.shards.ShardInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionLeaveShard implements ActionBase {

	private Boolean mAbandon;

	public ActionLeaveShard(JsonElement element) throws Exception {
		mAbandon = element.getAsBoolean();
		if (mAbandon == null) {
			throw new Exception("abandon value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		Core core = Core.getInstance();
		ShardInstance shardInstance = core.mShardManager.getShard(player);
		if (shardInstance != null) {
			shardInstance.removePlayer(player, mAbandon, true);
		}
	}
}
