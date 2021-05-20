package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.point.AreaBounds;
import com.playmonumenta.scriptedquests.point.Point;
import com.playmonumenta.scriptedquests.scriptedtimer.PlayerTimerData;
import com.playmonumenta.scriptedquests.scriptedtimer.Timer;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class PrerequisiteTimerCooldown implements PrerequisiteBase {

	private String mTimerId;
	private Set<String> mTimerTags = new HashSet<>();
	public PrerequisiteTimerCooldown(JsonElement value) throws Exception {
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
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			Timer timer = Plugin.getInstance().mTimerManager.getTimer(mTimerId);
			if (timer != null) {
				PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());

				PlayerTimerData timerData = null;
				for (PlayerTimerData t : data.getTimerData()) {
					if (t.mId.equalsIgnoreCase(timer.mId)) {
						timerData = t;
						break;
					}
				}

				if (timerData != null) {
					for (String str : mTimerTags) {

						// The player has this timer tag attached to them
						if (timerData.mTimerTags.contains(str)) {
							// Check to see if the timer is on cooldown at the moment.
							return timerData.mResetCounter == timer.getResetCounter();
						}
					}
				}
			}
		}

		return false;
	}
}
