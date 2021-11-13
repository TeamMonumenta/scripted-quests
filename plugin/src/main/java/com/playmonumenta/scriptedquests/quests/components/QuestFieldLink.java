package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class QuestFieldLink {

	public final String mQuestId;

	public final String mFieldId;
	public final String mDisplay;
	public final Boolean mVisible;
	public final Integer mMax;
	public final Boolean mDisplayNumber;
	public final String mAddon;

	private Map<Integer, QuestActions> mStages = new HashMap<>();
	private QuestPrerequisites mPrerequisites;
	private QuestActions mActions;
	public QuestFieldLink(String questId, JsonElement value) throws Exception {
		mQuestId = questId;
		JsonObject object = value.getAsJsonObject();

		if (object == null) {
			throw new Exception("quest_field value is not a object!");
		}

		mFieldId = object.get("field_id").getAsString();
		if (mFieldId == null) {
			throw new Exception("field_id value is not a string!");
		}

		mDisplay = object.get("field_name").getAsString();
		if (mDisplay == null) {
			throw new Exception("field_name value is not a string!");
		}

		mDisplayNumber = object.get("display_number").getAsBoolean();
		if (mDisplayNumber == null) {
			throw new Exception("display_number value is not a boolean!");
		}

		mVisible = object.get("visible").getAsBoolean();
		if (mVisible == null) {
			throw new Exception("visible value is not a boolean!");
		}

		mMax = object.get("max").getAsInt();
		if (mMax == null) {
			throw new Exception("max value is not a integer!");
		}

		mAddon = object.get("number_addon").getAsString();
		if (mAddon == null) {
			throw new Exception("number_addon value is not a string!");
		}

		if (object.has("stages")) {
			JsonArray stageArray = object.get("stages").getAsJsonArray();
			for (JsonElement element : stageArray) {
				JsonObject stageObject = element.getAsJsonObject();

				QuestActions actions = new QuestActions("", "", EntityType.PLAYER, 0, stageObject.get("actions"));
				mStages.put(stageObject.get("value").getAsInt(), actions);
			}
		}

		mPrerequisites = new QuestPrerequisites(object.get("prerequisites"));
		mActions = new QuestActions("", "", EntityType.PLAYER, 0, object.get("completion"));
	}

	public QuestPrerequisites getPrerequisites() {
		return mPrerequisites;
	}

	public QuestActions getActions() {
		return mActions;
	}

	public QuestActions getStageActions(int value) {
		return mStages.get(value);
	}

}
