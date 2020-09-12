package com.playmonumenta.scriptedquests.quests.components;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.EntityType;

public class QuestFieldLink {

	public final String mQuestId;

	public final String mFieldId;
	public final String mDisplay;
	public final Boolean mVisible;
	public final Boolean mDisplayNumber;
	public final String mAddon;

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

		mAddon = object.get("number_addon").getAsString();
		if (mAddon == null) {
			throw new Exception("number_addon value is not a string!");
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

}
