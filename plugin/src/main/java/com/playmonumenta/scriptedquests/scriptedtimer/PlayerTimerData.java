package com.playmonumenta.scriptedquests.scriptedtimer;

import com.google.gson.JsonObject;

public class PlayerTimerData {

	public final String mId;
	public int mResetCounter;

	public PlayerTimerData(JsonObject json) {
		mId = json.get("id").getAsString();
		mResetCounter = json.get("resetCounter").getAsInt();
	}

	public PlayerTimerData(String id) {
		mId = id;
		mResetCounter = 0;
	}

	public JsonObject getAsJsonObject() {
		JsonObject json = new JsonObject();
		json.addProperty("id", mId);
		json.addProperty("resetCounter", mResetCounter);
		return json;
	}
}
