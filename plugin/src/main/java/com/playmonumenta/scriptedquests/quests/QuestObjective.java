package com.playmonumenta.scriptedquests.quests;

import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class QuestObjective {

	private final String mId;
	private final String mDescription;
	private QuestPrerequisites mPrerequisites;
	private final boolean mVisible;
	private final boolean mDisplayNumber;
	private final int mObjectiveMax;
	private final int mObjectiveDefault;

	public QuestObjective(JsonObject object, boolean legacy) {

		if (!legacy) {
			mId = object.get("objective_id").getAsString();
			mDescription = object.get("objective_description").getAsString();

			try {
				mPrerequisites = new QuestPrerequisites(object.get("prerequisites"));
			} catch (Exception e) {
				e.printStackTrace();
				mPrerequisites = null;
			}

			mVisible = object.get("visible").getAsBoolean();
			mDisplayNumber = object.get("display_number").getAsBoolean();
			mObjectiveMax = object.get("objective_max").getAsInt();

			if (object.has("objective_default")) {
				mObjectiveDefault = object.get("objective_default").getAsInt();
			} else {
				mObjectiveDefault = 0;
			}
		} else {
			mId = object.get("field_id").getAsString();
			mDescription = object.get("field_name").getAsString();
			mDisplayNumber = object.get("display_number").getAsBoolean();
			mObjectiveMax = object.get("max").getAsInt();
			mVisible = object.get("visible").getAsBoolean();

			try {
				mPrerequisites = new QuestPrerequisites(object.get("prerequisites"));
			} catch (Exception e) {
				e.printStackTrace();
				mPrerequisites = null;
			}
			mObjectiveDefault = 0;
		}

	}

	public String getId() {
		return mId;
	}

	public String getDescription() {
		return mDescription;
	}

	public boolean isVisible() {
		return mVisible;
	}

	public boolean shouldDisplayNumber() {
		return mDisplayNumber;
	}

	public int getObjectiveMax() {
		return mObjectiveMax;
	}

	public int getObjectiveDefault() {
		return mObjectiveDefault;
	}

}
