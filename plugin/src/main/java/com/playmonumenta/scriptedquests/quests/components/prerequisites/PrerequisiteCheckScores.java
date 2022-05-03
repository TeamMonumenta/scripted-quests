package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;

public class PrerequisiteCheckScores implements PrerequisiteBase {
	private interface TestValue {
		int get(@Nullable Entity entity);
	}

	private static class TestValueConst implements TestValue {
		int mVal;

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
		String mObjective;

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

	private static class CheckScoreExact implements CheckScore {
		TestValue mVal;

		private CheckScoreExact(JsonElement val) throws Exception {
			mVal = parseTestValue(val);
		}

		@Override
		public boolean check(Entity entity, String scoreName) {
			return ScoreboardUtils.getScoreboardValue(entity, scoreName) == mVal.get(entity);
		}
	}

	private static class CheckScoreRange implements CheckScore {
		TestValue mMin;
		TestValue mMax;

		private CheckScoreRange(@Nullable JsonElement min, @Nullable JsonElement max) throws Exception {
			if (min == null) {
				mMin = parseTestValue(min);
			} else {
				mMin = new TestValueConst(Integer.MIN_VALUE);
			}
			if (max == null) {
				mMax = parseTestValue(max);
			} else {
				mMax = new TestValueConst(Integer.MAX_VALUE);
			}

			if (mMin instanceof TestValueConst cMin &&
				mMax instanceof TestValueConst cMax) {
				if (cMin.get() == Integer.MIN_VALUE &&
					cMax.get() == Integer.MAX_VALUE) {
					throw new Exception("Bogus check_score object with no min or max");
				}
			}
		}

		@Override
		public boolean check(Entity entity, String scoreName) {
			int value = ScoreboardUtils.getScoreboardValue(entity, scoreName);
			return value >= mMin.get(entity) && value <= mMax.get(entity);
		}
	}

	private String mScoreName;
	private CheckScore mCheckScore;

	public PrerequisiteCheckScores(String scoreName, JsonElement value) throws Exception {
		mScoreName = scoreName;

		if (value.isJsonPrimitive()) {
			//  Single value
			mCheckScore = new CheckScoreExact(value);
		} else {
			// Range of values
			@Nullable JsonElement rangeMin = null;
			@Nullable JsonElement rangeMax = null;

			Set<Entry<String, JsonElement>> subentries = value.getAsJsonObject().entrySet();
			for (Entry<String, JsonElement> subent : subentries) {
				String rangeKey = subent.getKey();

				if (rangeKey.equals("min")) {
					rangeMin = subent.getValue();
				} else if (rangeKey.equals("max")) {
					rangeMax = subent.getValue();
				} else {
					throw new Exception("Unknown check_score range key: '" + rangeKey + "'");
				}
			}

			mCheckScore = new CheckScoreRange(rangeMin, rangeMax);
		}
	}

	private static TestValue parseTestValue(JsonElement value) throws Exception {
		if (!value.isJsonPrimitive()) {
			throw new Exception("Test value is not a primitive");
		}

		// First try to parse the item as an integer
		@Nullable Integer valueAsInt;
		try {
			valueAsInt = value.getAsInt();
		} catch (Exception e) {
			valueAsInt = null;
		}
		if (valueAsInt != null) {
			return new TestValueConst(valueAsInt);
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
