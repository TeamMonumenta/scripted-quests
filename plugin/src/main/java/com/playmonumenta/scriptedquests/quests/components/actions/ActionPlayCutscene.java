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

public class ActionPlayCutscene implements ActionBase {

	private String mCutsceneId;
	private Location mLocation;
	public ActionPlayCutscene(JsonElement value) throws Exception {

		JsonObject json = value.getAsJsonObject();

		mCutsceneId = json.get("cutscene_id").getAsString();
		if (mCutsceneId == null) {
			throw new Exception("cutscene_id is an invald string!");
		}

		try {
			mProfessionType = ProfessionType.valueOf(json.get("professionType").getAsString());

			String locStr = json.get("location").getAsString();
			if (locStr == null) {
				throw new Exception("location value is not a string!");
			}

			mLocation = Utils.getLocation(locStr, false);
		} catch (IllegalArgumentException e) {
			throw new Exception("profession value is an invalid profession!");
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
		ProfessionData professionData = data.getProfessionData(mProfessionType);

		if (professionData != null) {
			if (professionData.mRecipes.size() > 0) {
				professionData.openProfessionMenu(Core.getInstance(), player, mLocation,
					0, professionData.mRecipes.get(0));
			} else {
				FormattedMessage.sendMessage(player, MessageFormat.PROFESSIONS, ChatColor.RED + "You need to learn some Recipes for this profession first!");
			}

		} else {
			FormattedMessage.sendMessage(player, MessageFormat.PROFESSIONS, ChatColor.RED + "You do not have this profession learned.");
		}
	}
}
