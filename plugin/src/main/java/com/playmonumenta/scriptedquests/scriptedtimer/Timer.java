package com.playmonumenta.scriptedquests.scriptedtimer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Calendar;

public class Timer {

	public final Plugin mPlugin;
	public final String mId;
	public final DayOfWeek mDayOfWeek;
	public final double mHours;

	public final TimerData mTimerData;
	public Timer(Plugin plugin, JsonElement value) throws Exception {
		mPlugin = plugin;

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

		// Read Data File
		File dataFolder = new File(plugin.getDataFolder() + File.separator + "timers" + File.separator + "data");
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}

		File dataFile = new File(plugin.getDataFolder() + File.separator + "timers" + File.separator + "data", mId + ".json");
		if (dataFile.exists()) {
			String content = FileUtils.readFile(dataFile.getPath());
			if (content == null || content.isEmpty()) {
				throw new Exception("Failed to parse file as JSON object");
			}

			Gson gson = new Gson();
			JsonObject object = gson.fromJson(content, JsonObject.class);
			if (object == null) {
				throw new Exception("Failed to parse file as JSON object");
			}

			mTimerData = new TimerData(this, object);
		} else {
			mTimerData = new TimerData(this);
		}
	}

	public int getResetCounter() {
		return mTimerData.mResetCounter;
	}

	public void updateTimer(long currentTime) {
		if (currentTime - mTimerData.mLastReset >= mTimerData.mResetInterval) {
			mTimerData.mLastReset += mTimerData.mResetInterval;
			mTimerData.mResetCounter++;

			// Recursive in case we need to update it multiple times to catch it up.
			updateTimer(currentTime);
		}
	}

	public void saveTimerData() {
		try {
			String path = mPlugin.getDataFolder() + File.separator + "timers" + File.separator + "data" + File.separator + mId + ".json";
			FileUtils.writeFile(path, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(mTimerData.getAsJsonObject()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
