package com.playmonumenta.scriptedquests.scriptedtimer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.TimeZone;
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
		calendar.setFirstDayOfWeek(DayOfWeek.SUNDAY.getValue());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.setTimeZone(TimeZone.getTimeZone("GMT-5"));
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.DAY_OF_WEEK, mTimer.mDayOfWeek.getValue());

		mResetInterval = (long) (mTimer.mHours * 60 * 60 * 1000);

//		Bukkit.broadcastMessage(mTimer.mId + " interval: " + TimeUnit.MILLISECONDS.toMinutes(mResetInterval));
//		Bukkit.broadcastMessage(mTimer.mId + " calendar: " + calendar.toString());
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
