package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.Map.Entry;
import java.util.Set;
import org.bukkit.entity.Entity;

public class PrerequisiteCheckScores implements PrerequisiteBase {
	private interface CheckScore {
		boolean check(Entity entity, String scoreName);
	}

	private static class CheckScoreExact implements CheckScore {
		int mVal;

		private CheckScoreExact(int val) {
			mVal = val;
		}

		@Override
		public boolean check(Entity entity, String scoreName) {
			return ScoreboardUtils.getScoreboardValue(entity, scoreName) == mVal;
		}
	}

	private static class CheckScoreOther implements CheckScore {
		String mOtherScore;

		private CheckScoreOther(String otherScore) {
			mOtherScore = otherScore;
		}

		@Override
		public boolean check(Entity entity, String scoreName) {
			return ScoreboardUtils.getScoreboardValue(entity, scoreName) == ScoreboardUtils.getScoreboardValue(entity, mOtherScore);
		}
	}

	private static class CheckScoreRange implements CheckScore {
		int mMin;
		int mMax;

		private CheckScoreRange(int min, int max) {
			mMin = min;
			mMax = max;
		}

		@Override
		public boolean check(Entity entity, String scoreName) {
			int value = ScoreboardUtils.getScoreboardValue(entity, scoreName);
			return value >= mMin && value <= mMax;
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
				mCheckScore = new CheckScoreExact(valueAsInt);
			} catch (Exception e) {
				// If that failed, try a string instead
				String valueAsString = value.getAsString();
				if (valueAsString != null) {
					mCheckScore = new CheckScoreOther(valueAsString);
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

			mCheckScore = new CheckScoreRange(imin, imax);
		}
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		return mCheckScore.check(context.getEntityUsedForPrerequisites(), mScoreName);
	}
}
