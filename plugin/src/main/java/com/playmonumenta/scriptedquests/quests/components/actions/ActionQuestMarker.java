package com.playmonumenta.scriptedquests.quests.components.actions;

import com.google.gson.JsonObject;

public class ActionQuestMarker {

	private String mMarker = null;
	public ActionQuestMarker(JsonObject jsonObject) {
		if (jsonObject.has("quest_marker")) {
			mMarker = jsonObject.get("quest_marker").getAsString();
		}
	}

	public String getMarker() {
		return mMarker;
	}

}
