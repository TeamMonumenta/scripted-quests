package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.Novalescent.Core;
import me.Novalescent.player.PlayerData;
import me.Novalescent.player.quests.QuestData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrerequisiteCheckQuestData implements PrerequisiteBase {

	private class CheckField {
		// This should be an enum, but idk how to set those up. -Nick
		private int mOperation;
		private static final int CHECK_EXACT = 1;
		private static final int CHECK_OTHER = 2;
		private static final int CHECK_RANGE = 3;

		String mOtherScore;
		int mMin;
		int mMax;
		String mFieldName;

		CheckField(String fieldName, int value) {
			mMin = value;
			mFieldName = fieldName;
			mOperation = CHECK_EXACT;
		}

		CheckField(String value) {
			mOtherScore = value;
			mOperation = CHECK_OTHER;
		}

		CheckField(String fieldName, int min, int max) {
			mMin = min;
			mMax = max;
			mFieldName = fieldName;
			mOperation = CHECK_RANGE;

		}

		boolean check(Player player, String questId) {
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			QuestData questData = data.getQuestData(questId);

			if (questData != null) {

				int value = questData.getEntry(mFieldName);
				switch (mOperation) {
					case CHECK_EXACT:
						return value == mMin;
					case CHECK_RANGE:
						return value >= mMin && value <= mMax;
					default:
						return false;
				}
			}
			return false;
		}

	}

	private String mId;
	private Integer mCompletedCheck = 0;
	private List<CheckField> mChecks;
	public PrerequisiteCheckQuestData(JsonElement value) throws Exception {
		JsonObject object = value.getAsJsonObject();
		if (object == null) {
			throw new Exception("quest data value is not an object!");
		}

		mChecks = new ArrayList<>();

		mId = object.get("quest_id").getAsString();
		if (mId == null) {
			throw new Exception("quest_id value is not a string!");
		}

		if (object.has("completed")) {
			mCompletedCheck = object.get("completed").getAsInt();
		}

		JsonObject fields = object.get("quest_data_fields").getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
			JsonElement fieldValue = fields.get(entry.getKey());

			if (fieldValue.isJsonPrimitive()) {
				mChecks.add(new CheckField(entry.getKey(), fieldValue.getAsInt()));
			} else {
				// Range of values
				Integer imin = Integer.MIN_VALUE;
				Integer imax = Integer.MAX_VALUE;

				Set<Map.Entry<String, JsonElement>> subentries = fieldValue.getAsJsonObject().entrySet();
				for (Map.Entry<String, JsonElement> subent : subentries) {
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

				mChecks.add(new CheckField(entry.getKey(), imin, imax));
			}
		}

	}

	@Override
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			PlayerData data = Core.getInstance().mPlayerManager.getPlayerData(player.getUniqueId());
			QuestData questData = data.getQuestData(mId);

			System.out.println(mId);
			if (questData != null) {
				System.out.println("not null");
				System.out.println(mCompletedCheck);
				// Check for completion
				if (mCompletedCheck == 1) { // Is it completed?
					if (!questData.mCompleted) {
						return false;
					}
 				} else if (mCompletedCheck == 2) { // Is it NOT completed?

					if (questData.mCompleted) {
						System.out.println("completed, return false");
						return false;
					}
				}

				for (CheckField check : mChecks) {
					if (!check.check(player, mId)) {
						return false;
					}
				}

				System.out.println("return true");
				return true;
			}
			return false;
		}
		return false;
	}
}
