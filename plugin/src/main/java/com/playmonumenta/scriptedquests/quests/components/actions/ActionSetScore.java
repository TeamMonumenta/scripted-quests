package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import org.bukkit.entity.Player;

public class ActionSetScore implements ActionBase {
	private static class SetScore {
		// This should be an enum, but idk how to set those up. -Nick
		private int mOperation;
		private static final int SET_EXACT = 1;
		private static final int SET_COPY = 2;
		private static final int SET_RANDOM = 3;

		private String mSourceScore;
		private int mValue;
		private int mValueRange; // max - min + 1
		private Random mRandom = new Random();

		SetScore(int value) {
			mValue = value;
			mOperation = SET_EXACT;
		}

		SetScore(String sourceScore) {
			mSourceScore = sourceScore;
			mOperation = SET_COPY;
		}

		SetScore(int minValue, int maxValue) {
			mValue = minValue;
			mValueRange = maxValue - minValue + 1;
			mOperation = SET_RANDOM;
		}

		void apply(Player player, String targetScore) {
			int tempScore;
			switch (mOperation) {
			case SET_COPY:
				tempScore = ScoreboardUtils.getScoreboardValue(player, mSourceScore);
				ScoreboardUtils.setScoreboardValue(player, targetScore, tempScore);
				break;
			case SET_RANDOM:
				tempScore = mValue + mRandom.nextInt(mValueRange);
				ScoreboardUtils.setScoreboardValue(player, targetScore, tempScore);
				break;
			case SET_EXACT:
			default:
				ScoreboardUtils.setScoreboardValue(player, targetScore, mValue);
				break;
			}
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
				mSetScore = new SetScore(valueAsInt);
			} catch (Exception e) {
				// If that failed, try a string instead
				String valueAsString = value.getAsString();
				if (valueAsString != null) {
					mSetScore = new SetScore(valueAsString);
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

			mSetScore = new SetScore(imin, imax);
		}
	}

	@Override
	public void doAction(QuestContext context) {
		mSetScore.apply(context.getPlayer(), mScoreName);
	}
}
