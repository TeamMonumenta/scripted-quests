package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.abilities.core.Ability;
import me.Novalescent.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionGiveAbility implements ActionBase {

	private String mAbilityId;

	public ActionGiveAbility(JsonElement element) throws Exception {
		mAbilityId = element.getAsString();
		if (mAbilityId == null) {
			throw new Exception("Ability value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		player.sendMessage(ChatColor.RED + "ERROR: GiveAbility is an old action, and is no longer used/functional. Please report this.");
		Ability ability = Core.getInstance().mAbilityManager.getAbility(mAbilityId, player);

		if (ability != null) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
		}

	}
}
