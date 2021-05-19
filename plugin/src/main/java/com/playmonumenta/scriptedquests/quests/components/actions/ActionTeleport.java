package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionTeleport implements ActionBase {

	private String mLocString;
	public ActionTeleport(JsonElement value) throws Exception {
		mLocString = value.getAsString();

		if (mLocString == null) {
			throw new Exception("loc value is not a string!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		Location loc = Utils.getLocation(mLocString, true);
		if (loc.isWorldLoaded()) {
			player.teleport(loc);
		}
	}
}
