package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import org.bukkit.entity.Player;

public class ActionSetScore implements ActionBase {
	private interface SetScore {
		void apply(Player player, String targetScore);
	}

	private static class SetScoreExact implements SetScore {
		final int mValue;

		private SetScoreExact(int val) {
			mValue = val;
		}

		@Override
		public void apply(Player player, String targetScore) {
			ScoreboardUtils.setScoreboardValue(player, targetScore, mValue);
		}
	}

	private static class SetScoreCopy implements SetScore {
		final String mSourceScore;

		private SetScoreCopy(String sourceScore) {
			mSourceScore = sourceScore;
		}

		@Override
		public void apply(Player player, String targetScore) {
			int tempScore = ScoreboardUtils.getScoreboardValue(player, mSourceScore);
			ScoreboardUtils.setScoreboardValue(player, targetScore, tempScore);
		}
	}

	private static class SetScoreRandom implements SetScore {
		private static final Random RANDOM = new Random();
		final int mMinValue;
		final int mValueRange; // max - min + 1

		private SetScoreRandom(int minValue, int maxValue) {
			mMinValue = minValue;
			mValueRange = maxValue - minValue + 1;
		}

		@Override
		public void apply(Player player, String targetScore) {
			int tempScore = mMinValue + RANDOM.nextInt(mValueRange);
			ScoreboardUtils.setScoreboardValue(player, targetScore, tempScore);
		}
	}

	private String mScoreName;
	private SetScore mSetScore;

	public ActionSetScore(String scoreName, JsonElement value) throws Exception {
		mScoreName = scoreName;

		if (value.isJsonPrimitive()) {
			//  Single value

			// First try to parse the item as an integer
			try {
				int valueAsInt = value.getAsInt();
				mSetScore = new SetScoreExact(valueAsInt);
			} catch (Exception e) {
				// If that failed, try a string instead
				String valueAsString = value.getAsString();
				if (valueAsString != null) {
					mSetScore = new SetScoreCopy(valueAsString);
				} else {
					throw new Exception("set_score value for scoreboard '" + mScoreName +
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

				switch (rangeKey) {
				case "min":
					imin = subent.getValue().getAsInt();
					break;
				case "max":
					imax = subent.getValue().getAsInt();
					break;
				default:
					throw new Exception("Unknown check_score value: '" + rangeKey + "'");
				}
			}

			if (imin == Integer.MIN_VALUE && imax == Integer.MAX_VALUE) {
				throw new Exception("Bogus check_score object with no min or max");
			}

			mSetScore = new SetScoreRandom(imin, imax);
		}
	}

	@Override
	public void doActions(QuestContext context) {
		mSetScore.apply(context.getPlayer(), mScoreName);
	}
}
