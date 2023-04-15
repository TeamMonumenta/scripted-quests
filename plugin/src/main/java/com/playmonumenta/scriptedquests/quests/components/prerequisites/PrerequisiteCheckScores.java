package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Objective;

public class PrerequisiteCheckScores implements PrerequisiteBase {
	private interface TestValue {
		int get(@Nullable Entity entity);
	}

	private static class TestValueConst implements TestValue {
		final int mVal;

		private TestValueConst(int val) {
			mVal = val;
		}

		@Override
		public int get(@Nullable Entity entity) {
			return mVal;
		}

		public int get() {
			return mVal;
		}
	}

	private static class TestValueObjective implements TestValue {
		final String mObjective;

		private TestValueObjective(String objective) throws Exception {
			if (!ScoreboardUtils.isValidObjective(objective)) {
				throw new Exception("Invalid objective name '" + objective + "'");
			}
			mObjective = objective;
		}

		@Override
		public int get(@Nullable Entity entity) {
			if (entity == null) {
				return 0;
			}
			return ScoreboardUtils.getScoreboardValue(entity, mObjective);
		}
	}

	private interface CheckScore {
		boolean check(Entity entity, String scoreName);
	}

	private static class CheckScoreSimple implements CheckScore {
		final TestValue mVal;

		private CheckScoreSimple(JsonElement val) throws Exception {
			mVal = parseTestValue(val);
		}

		@Override
		public boolean check(Entity entity, String scoreName) {
			return ScoreboardUtils.getScoreboardValue(entity, scoreName) == mVal.get(entity);
		}
	}

	private static class CheckScoreExtended implements CheckScore {
		final TestValue mMin;
		final TestValue mMax;
		final @Nullable String testScoreboardHolder;

		private CheckScoreExtended(@Nullable JsonElement min, @Nullable JsonElement max, @Nullable JsonElement scoreboardHolder) throws Exception {
			if (min == null) {
				mMin = new TestValueConst(Integer.MIN_VALUE);
			} else {
				mMin = parseTestValue(min);
			}
			if (max == null) {
				mMax = new TestValueConst(Integer.MAX_VALUE);
			} else {
				mMax = parseTestValue(max);
			}
			if (scoreboardHolder != null) {
				testScoreboardHolder = scoreboardHolder.getAsString();
			} else {
				testScoreboardHolder = null;
			}

			if (mMin instanceof TestValueConst cMin &&
				    mMax instanceof TestValueConst cMax) {
				if (cMin.get() == Integer.MIN_VALUE &&
					    cMax.get() == Integer.MAX_VALUE) {
					throw new Exception("Bogus check_score object with no min, max, or value");
				}
			}
		}

		@Override
		public boolean check(Entity entity, String scoreName) {
			int value;
			if (testScoreboardHolder == null) {
				value = ScoreboardUtils.getScoreboardValue(entity, scoreName);
			} else {
				Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(scoreName);
				if (objective != null) {
					value = objective.getScore(testScoreboardHolder).getScore();
				} else {
					value = 0;
				}
			}
			return value >= mMin.get(entity) && value <= mMax.get(entity);
		}
	}

	private final String mScoreName;
	private final CheckScore mCheckScore;

	public PrerequisiteCheckScores(String scoreName, JsonElement value) throws Exception {
		mScoreName = scoreName;

		if (value.isJsonPrimitive()) {
			// Single value
			mCheckScore = new CheckScoreSimple(value);
		} else {
			// Range of values
			@Nullable JsonElement rangeMin = null;
			@Nullable JsonElement rangeMax = null;
			@Nullable JsonElement scoreboardHolder = null;

			Set<Entry<String, JsonElement>> subentries = value.getAsJsonObject().entrySet();
			for (Entry<String, JsonElement> subent : subentries) {
				String key = subent.getKey();

				switch (key) {
					case "min" -> rangeMin = subent.getValue();
					case "max" -> rangeMax = subent.getValue();
					case "value" -> {
						rangeMin = subent.getValue();
						rangeMax = subent.getValue();
					}
					case "scoreboard_holder" -> scoreboardHolder = subent.getValue();
					default -> throw new Exception("Unknown check_score key: '" + key + "'");
				}
			}

			mCheckScore = new CheckScoreExtended(rangeMin, rangeMax, scoreboardHolder);
		}
	}

	private static TestValue parseTestValue(JsonElement value) throws Exception {
		if (value == null || !value.isJsonPrimitive()) {
			throw new Exception("Test value is not a primitive");
		}

		// First try to parse the item as an integer
		try {
			return new TestValueConst(value.getAsInt());
		} catch (Exception e) {
			// ignore
		}

		// If that failed, try a string instead
		@Nullable String valueAsString = value.getAsString();
		if (valueAsString != null) {
			return new TestValueObjective(valueAsString);
		} else {
			throw new Exception("Test value is neither an integer nor a string!");
		}
	}

	@Override
	public boolean prerequisiteMet(QuestContext context) {
		return mCheckScore.check(context.getEntityUsedForPrerequisites(), mScoreName);
	}
}
