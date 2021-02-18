package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionGiveXP implements ActionBase {

	private Integer mXp;
	public ActionGiveXP(JsonElement value) throws Exception {
		mXp = value.getAsInt();

		if (mXp == null) {
			throw new Exception("XP value is not an integer!");
		}
	}

	private String bracketText(String str) {
		return ChatColor.DARK_GRAY + "[" + str + ChatColor.DARK_GRAY + "]";
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
		data.giveXP(mXp);
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.15f);
		player.sendMessage(bracketText(Utils.getColor("#73deff") + "+"
			+ mXp + " " + Utils.getColor("#93ff73") + "Experience Points"));
	}
}
