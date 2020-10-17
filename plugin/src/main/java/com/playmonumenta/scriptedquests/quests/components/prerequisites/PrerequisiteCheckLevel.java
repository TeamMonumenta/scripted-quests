package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public class PrerequisiteCheckLevel implements PrerequisiteBase {

	private class CheckLevel {
		// This should be an enum, but idk how to set those up. -Nick
		private int mOperation;
		private static final int CHECK_EXACT = 1;
		private static final int CHECK_OTHER = 2;
		private static final int CHECK_RANGE = 3;

		String mOtherScore;
		int mMin;
		int mMax;

		CheckLevel(int value) {
			mMin = value;
			mOperation = CHECK_EXACT;
		}

		CheckLevel(String value) {
			mOtherScore = value;
			mOperation = CHECK_OTHER;
		}

		CheckLevel(int min, int max) {
			mMin = min;
			mMax = max;
			mOperation = CHECK_RANGE;
		}

		boolean check(Player player) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			int value = data.mClassData != null ? data.mClassData.mLevel : 0;
			switch (mOperation) {
				case CHECK_EXACT:
				case CHECK_OTHER:
					return value == mMin;
				case CHECK_RANGE:
					return value >= mMin && value <= mMax;
				default:
					return false;
			}
		}

	}

	private CheckLevel mCheckLevel;

	public PrerequisiteCheckLevel(JsonElement value) throws Exception {

		if (value.isJsonPrimitive()) {
			//  Single value

			// First try to parse the item as an integer
			try {
				int valueAsInt = value.getAsInt();
				mCheckLevel = new CheckLevel(valueAsInt);
			} catch (Exception e) {
				// If that failed, try a string instead
				String valueAsString = value.getAsString();
				if (valueAsString != null) {
					mCheckLevel = new CheckLevel(valueAsString);
				} else {
					throw new Exception("check_level value is neither an integer nor a string!");
				}
			}
		} else {
			// Range of values
			Integer imin = Integer.MIN_VALUE;
			Integer imax = Integer.MAX_VALUE;

			Set<Map.Entry<String, JsonElement>> subentries = value.getAsJsonObject().entrySet();
			for (Map.Entry<String, JsonElement> subent : subentries) {
				String rangeKey = subent.getKey();

				if (rangeKey.equals("min")) {
					imin = subent.getValue().getAsInt();
				} else if (rangeKey.equals("max")) {
					imax = subent.getValue().getAsInt();
				} else {
					throw new Exception("Unknown check_level value: '" + rangeKey + "'");
				}
			}

			if (imin == Integer.MIN_VALUE && imax == Integer.MAX_VALUE) {
				throw new Exception("Bogus check_level object with no min or max");
			}

			mCheckLevel = new CheckLevel(imin, imax);
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			return mCheckLevel.check(player);
		}
		return false;
	}
}
