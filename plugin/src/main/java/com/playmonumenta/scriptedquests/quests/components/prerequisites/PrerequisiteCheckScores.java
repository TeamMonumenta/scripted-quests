package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Entity;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

public class PrerequisiteCheckScores implements PrerequisiteBase {
	public enum OperationType {
		CHECK_EXACT,
		CHECK_OTHER,
		CHECK_RANGE;
	}

	private class CheckScore {
		// This should be an enum, but idk how to set those up. -Nick
		private OperationType mOperation;

		String mOtherScore;
		int mMin;
		int mMax;

		CheckScore(int value) {
			mMin = value;
			mOperation = OperationType.CHECK_EXACT;
		}

		CheckScore(String value) {
			mOtherScore = value;
			mOperation = OperationType.CHECK_OTHER;
		}

		CheckScore(int min, int max) {
			mMin = min;
			mMax = max;
			mOperation = OperationType.CHECK_RANGE;
		}

		boolean check(Entity entity, String scoreName) {
			int value = ScoreboardUtils.getScoreboardValue(entity, scoreName);
			switch (mOperation) {
			case CHECK_EXACT:
				return value == mMin;
			case CHECK_OTHER:
				mMin = ScoreboardUtils.getScoreboardValue(entity, mOtherScore);
				return value == mMin;
			case CHECK_RANGE:
				return value >= mMin && value <= mMax;
			default:
				return false;
			}
		}
	}

	private String mScoreName;
	private CheckScore mCheckScore;

	public PrerequisiteCheckScores(String scoreName, JsonElement value) throws Exception {
		mScoreName = scoreName;

		if (value.isJsonPrimitive()) {
			//  Single value

			// First try to parse the item as an integer
			try {
				int valueAsInt = value.getAsInt();
				mCheckScore = new CheckScore(valueAsInt);
			} catch (Exception e) {
				// If that failed, try a string instead
				String valueAsString = value.getAsString();
				if (valueAsString != null) {
					mCheckScore = new CheckScore(valueAsString);
				} else {
					throw new Exception("check_score value for scoreboard '" + mScoreName +
					                    "' is neither an integer nor a string!");
				}
			}
		} else {
			// Range of values
			Integer imin = Integer.MIN_VALUE;
			Integer imax = Integer.MAX_VALUE;

			Set<Entry<String, JsonElement>> subentries = value.getAsJsonObject().entrySet();
			for (Entry<String, JsonElement> subent : subentries) {
				String rangeKey = subent.getKey();

				if (rangeKey.equals("min")) {
					imin = subent.getValue().getAsInt();
				} else if (rangeKey.equals("max")) {
					imax = subent.getValue().getAsInt();
				} else {
					throw new Exception("Unknown check_score value: '" + rangeKey + "'");
				}
			}

			if (imin == Integer.MIN_VALUE && imax == Integer.MAX_VALUE) {
				throw new Exception("Bogus check_score object with no min or max");
			}

			mCheckScore = new CheckScore(imin, imax);
		}
	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		return mCheckScore.check(entity, mScoreName);
	}
}
