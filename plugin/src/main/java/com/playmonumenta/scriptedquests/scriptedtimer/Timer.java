package com.playmonumenta.scriptedquests.scriptedtimer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.DayOfWeek;
import java.util.Calendar;

public class Timer {

	public final String mId;
	public final DayOfWeek mDayOfWeek;
	public final double mHours;
	public Timer(JsonElement value) throws Exception {
		JsonObject json = value.getAsJsonObject();
		if (json == null) {
			throw new Exception("Value for a timer is not a JsonObject!");
		}

		if (json.has("id")) {
			mId = json.get("id").getAsString();
		} else {
			throw new Exception("JsonObject for a Timer does not have an id!");
		}

		mDayOfWeek = DayOfWeek.valueOf(json.get("dayOfTheWeek").getAsString());
		mHours = json.get("resetHours").getAsDouble();

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_WEEK, mDayOfWeek.getValue());
	}

}
