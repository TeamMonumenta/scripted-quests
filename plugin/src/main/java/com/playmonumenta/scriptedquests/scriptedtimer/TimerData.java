package com.playmonumenta.scriptedquests.scriptedtimer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class TimerData {

	public final Timer mTimer;
	public final long mResetInterval;

	public long mLastReset;

	public int mResetCounter;
	public TimerData(Timer timer) {
		mTimer = timer;

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_WEEK, mTimer.mDayOfWeek.getValue());

		mResetInterval = (long) (calendar.getTimeInMillis() + (mTimer.mHours * 60 * 60 * 1000)) - calendar.getTimeInMillis();

		mLastReset = calendar.getTimeInMillis();
	}

	public TimerData(Timer timer, JsonObject value) {
		this(timer);

		mLastReset = value.get("lastReset").getAsLong();
		mResetCounter = value.get("resetCounter").getAsInt();
	}

	public JsonObject getAsJsonObject() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("lastReset", mLastReset);
		jsonObject.addProperty("resetCounter", mResetCounter);
		return jsonObject;
	}
}
