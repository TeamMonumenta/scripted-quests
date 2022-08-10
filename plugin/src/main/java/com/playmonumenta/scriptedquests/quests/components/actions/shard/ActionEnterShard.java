package com.playmonumenta.scriptedquests.quests.components.actions.shard;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.quests.components.actions.ActionBase;
import me.Novalescent.Core;
import me.Novalescent.abilities.core.Ability;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionEnterShard implements ActionBase {

	private String mShardId;

	public ActionEnterShard(JsonElement element) throws Exception {
		mShardId = element.getAsString();
		if (mShardId == null) {
			throw new Exception("Shard value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		Core core = Core.getInstance();
		core.mShardManager.enterShard(player, core.mShardManager.getShard(mShardId));
	}
}
