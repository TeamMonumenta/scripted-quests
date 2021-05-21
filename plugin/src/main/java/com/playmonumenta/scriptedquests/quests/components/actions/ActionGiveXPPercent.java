package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.utils.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionGiveXPPercent implements ActionBase {

	// XP Percent
	private Integer mXPLevel;
	private double mXPPercent;
	public ActionGiveXPPercent(JsonElement value) throws Exception {
		if (!value.isJsonObject()) {
			throw new Exception("xp_percent is not an object!");
		}
		JsonObject jsonObject = value.getAsJsonObject();
		mXPLevel = jsonObject.get("level").getAsInt();
		mXPPercent = jsonObject.get("percent").getAsDouble();
	}

	private String bracketText(String str) {
		return ChatColor.DARK_GRAY + "[" + str + ChatColor.DARK_GRAY + "]";
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		int xp = (int) (PlayerData.getXPForLevel(mXPLevel) * mXPPercent);
		PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
		data.giveXP(xp);
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.15f);
		player.sendMessage(bracketText(Utils.getColor("#73deff") + "+"
			+ xp + " " + Utils.getColor("#93ff73") + "Experience Points"));
	}
}
