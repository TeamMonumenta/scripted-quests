package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.professions.ProfessionData;
import me.Novalescent.professions.ProfessionType;
import me.Novalescent.utils.FormattedMessage;
import me.Novalescent.utils.MessageFormat;
import me.Novalescent.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class ActionLearnProfession implements ActionBase {

	private ProfessionType mProfessionType;
	public ActionLearnProfession(JsonElement value) throws Exception {

		try {
			mProfessionType = ProfessionType.valueOf(value.getAsString());

		} catch (IllegalArgumentException e) {
			throw new Exception("profession value is an invalid profession!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		mProfessionType.learnProfession(Core.getInstance(), player);
	}
}
