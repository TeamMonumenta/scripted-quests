package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestData {

	/**
	 * The string ID of the QuestData
	 */
	public final String mId;

	/**
	 * The Quest Data entries along with their respective values. Each entry is {entry_id, entry_value}
	 */
	private final Map<String, Integer> mDataEntries = new HashMap<>();

	/**
	 * A list of all of the quest data entry keys in the order that they were parsed in (mDataEntries.keySet() does not return the original order)
	 */
	private final List<String> mKeys = new ArrayList<>();

	/**
	 * A list of the current Quest Stage progression that the player is on. The order of the Quest St
	 */
	private final List<QuestStageData> mStages = new ArrayList<>();

	/**
	 * The boolean for if this Quest is completed
	 */
	public boolean mCompleted = false;

	/**
	 * The boolean for if this Quest is being tracked. Only 1 QuestData can be tracked at a time per player
	 */
	public boolean mTracked = false;

	/**
	 * The stage that the quest is on currently
	 */
	private int mStage = 1;

	/**
	 * Constructs a new QuestData object using the given QuestData id
	 * @param id The id to us
	 */
	public QuestData(String id) {
		mId = id;
		mStages.add(new QuestStageData());
	}

	/**
	 * Constructs a new QuestData object by parsing a JsonObject. This is usually obtained from a save file
	 * @param json The JsonObject to parse and load from
	 */
	public QuestData(JsonObject json) {
		mId = json.get("quest_id").getAsString();

		if (json.has("completed")) {
			mCompleted = json.get("completed").getAsBoolean();
		}

		if (json.has("tracked")) {
			mTracked = json.get("tracked").getAsBoolean();
		}

		if (json.has("stage")) {
			mStage = json.get("stage").getAsInt();
		}

		// Legacy format
		if (json.has("entries")) {
			JsonObject entries = json.get("entries").getAsJsonObject();
			mStages.add(new QuestStageData(entries));
		} else {
			JsonArray stageArray = json.getAsJsonArray("stages");
			for (JsonElement element : stageArray) {
				mStages.add(new QuestStageData(element.getAsJsonObject()));
			}
		}

	}

	/**
	 * Constructs a new QuestData object by using a Quest object
	 * @param quest The quest to create the QuestData from.
	 */
	public QuestData(Quest quest) {
		mId = quest.getQuestId();
	}

	public List<String> getEntryKeys() {
		return mKeys;
	}

	public int getEntry(String entry) {
		if (mDataEntries.containsKey(entry)) {
			return mDataEntries.get(entry);
		}

		return -1;
	}

	public boolean changeEntry(String entry, int change) {
		Integer current = mDataEntries.get(entry);
		if (current == null) {
			current = 0;
		}
		int original = current;

		current += change;
		boolean max = false;
		QuestDataLink link = Plugin.getInstance().mQuestDataLinkManager.getQuestDataLink(mId);
		if (link != null) {
			QuestFieldLink fieldLink = link.getFieldLink(entry);
			if (fieldLink.mMax != -1 && current >= fieldLink.mMax) {
				max = true;

				// Check if it was already maxed
				if (current - change >= fieldLink.mMax) {
					max = false;
				}
				current = fieldLink.mMax;
			}

		}

		if (!mKeys.contains(entry)) {
			mKeys.add(0, entry);
		}
		mDataEntries.put(entry, current);
		return max;
	}

	public boolean setEntry(String entry, int set) {
		Integer current = mDataEntries.get(entry);
		if (current == null) {
			current = 0;
		}

		boolean max = false;
		QuestDataLink link = Plugin.getInstance().mQuestDataLinkManager.getQuestDataLink(mId);
		if (link != null) {
			QuestFieldLink fieldLink = link.getFieldLink(entry);
			if (fieldLink != null) {
				if (fieldLink.mMax != -1 && set >= fieldLink.mMax) {
					max = true;

					// Check if it just got maxed
					if (current < fieldLink.mMax) {
						max = false;
					}
					set = fieldLink.mMax;
				}
			}
		}

		if (!mKeys.contains(entry)) {
			mKeys.add(0, entry);
		}
		mDataEntries.put(entry, set);
		return max;
	}

	public int getStage() {
		return mStage;
	}

	public QuestStageData getStageData(int stageNumber) {
		int stageIndex = stageNumber - 1;
		if (stageIndex < 0) {
			return mStages.get(0);
		} else if (stageIndex >= mStages.size()) {
			return mStages.get(mStages.size() - 1);
		} else {
			return mStages.get(stageIndex);
		}
	}

	public void nextStage(Quest quest, boolean progressStage) {
		if (progressStage) {
			if (mStage < quest.getStages().size()) {
				mStage++;
			} else {
				return;
			}
		}

		QuestStage nextStage = quest.getStage(mStage);
		mStages.add(nextStage.getNewStageData(mId));
	}

	public JsonObject getAsJsonObject() {
		JsonObject json = new JsonObject();
		json.addProperty("quest_id", mId);
		json.addProperty("completed", mCompleted);
		json.addProperty("stage", mStage);

		if (mTracked) {
			json.addProperty("tracked", true);
		}

		JsonArray stageArray = new JsonArray();
		for (QuestStageData stageData : mStages) {
			stageArray.add(stageData.getAsJsonObject());
		}

		json.add("stages", stageArray);

		// Legacy saving
//		JsonObject entries = new JsonObject();
//		for (Map.Entry<String, Integer> entry : mDataEntries.entrySet()) {
//			entries.addProperty(entry.getKey(), entry.getValue());
//		}
//
//		json.add("entries", entries);
		return json;
	}

}
