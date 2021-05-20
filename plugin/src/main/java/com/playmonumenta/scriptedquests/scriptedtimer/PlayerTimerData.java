package com.playmonumenta.scriptedquests.scriptedtimer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

public class PlayerTimerData {

	public final String mId;
	public int mResetCounter;
	public Set<String> mTimerTags = new HashSet<>();

	public PlayerTimerData(JsonObject json) {
		mId = json.get("id").getAsString();
		mResetCounter = json.get("resetCounter").getAsInt();

		if (json.has("timer_tags")) {
			JsonArray timerTags = json.get("timer_tags").getAsJsonArray();
			for (JsonElement element : timerTags) {
				mTimerTags.add(element.getAsString());
			}
		}
	}

	public PlayerTimerData(String id) {
		mId = id;
		mResetCounter = 0;
	}

	public JsonObject getAsJsonObject() {
		JsonObject json = new JsonObject();
		json.addProperty("id", mId);
		json.addProperty("resetCounter", mResetCounter);

		JsonArray tagArray = new JsonArray();
		for (String str : mTimerTags) {
			tagArray.add(str);
		}
		json.add("timer_tags", tagArray);
		return json;
	}
}
