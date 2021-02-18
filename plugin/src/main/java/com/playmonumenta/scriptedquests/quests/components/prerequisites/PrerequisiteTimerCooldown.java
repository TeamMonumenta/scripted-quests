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

import java.util.Map.Entry;
import java.util.Set;

public class PrerequisiteTimerCooldown implements PrerequisiteBase {
	private String mTimerId;

	public PrerequisiteTimerCooldown(JsonElement element) throws Exception {
		mTimerId = element.getAsString();
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
					return timer.getResetCounter() == timerData.mResetCounter;
				}
			}
		}

		return false;
	}
}
