package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;
import com.playmonumenta.scriptedquests.scriptedtimer.PlayerTimerData;
import com.playmonumenta.scriptedquests.scriptedtimer.Timer;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ActionTimerCooldown implements ActionBase {

	private String mTimerId;
	private Set<String> mTimerTags = new HashSet<>();
	public ActionTimerCooldown(JsonElement value) throws Exception {
		JsonObject json = value.getAsJsonObject();
		if (json == null) {
			throw new Exception("timer value is not a json object!");
		}

		mTimerId = json.get("timer_id").getAsString();

		for (JsonElement element : json.get("timer_tags").getAsJsonArray()) {
			mTimerTags.add(element.getAsString());
		}
	}

	@Override
	public void doAction(Plugin plugin, Player player, Entity npcEntity, QuestPrerequisites prereqs) {
		Timer timer = plugin.mTimerManager.getTimer(mTimerId);
		if (timer != null) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());

			PlayerTimerData timerData = null;
			for (PlayerTimerData t : data.getTimerData()) {
				if (t.mId.equalsIgnoreCase(timer.mId)) {
					timerData = t;
					break;
				}
			}

			if (timerData == null) {
				timerData = new PlayerTimerData(timer.mId);
				data.getTimerData().add(timerData);
			}

			timerData.mResetCounter = timer.getResetCounter();
			timerData.mTimerTags.addAll(mTimerTags);
			data.updateQuestVisibility();
		}
	}
}
