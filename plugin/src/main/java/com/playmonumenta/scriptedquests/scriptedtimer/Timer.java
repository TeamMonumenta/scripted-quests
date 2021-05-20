package com.playmonumenta.scriptedquests.scriptedtimer;

import com.google.gson.*;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.utils.FileUtils;
import me.Novalescent.utils.quadtree.reworked.QuadTree;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

		if (json.has("timerCounters")) {
			double x, y, z = 0;
			// Spawn @ Locations
			JsonArray array = json.get("timerCounters").getAsJsonArray();
			for (JsonElement element : array) {
				JsonObject locObject = element.getAsJsonObject();

				if (locObject == null) {
					throw new Exception("locations array entry is not a valid location");
				}

				x = 0;
				y = 0;
				z = 0;

				World world = null;
				String message = "@T";
				for (Map.Entry<String, JsonElement> ent : locObject.entrySet()) {
					String key = ent.getKey();
					JsonElement v = ent.getValue();

					switch (key) {

						case "world":
							world = Bukkit.getWorld(v.getAsString());
							break;

						case "x":
							x = v.getAsDouble();
							break;

						case "y":
							y = v.getAsDouble();
							break;

						case "z":
							z = v.getAsDouble();
							break;

						case "message":
							message = v.getAsString();
							break;

						default:
							throw new Exception("Unknown locations key: '" + key + "'");
					}
				}

				if (world != null) {
					Vector vec = new Vector(x, y, z);
					Location loc = vec.toLocation(world);

					TimerTreeNode node = new TimerTreeNode(locObject.get("timer_tag").getAsString(), loc, this, message);
					QuadTree<TimerTreeNode> quadTree = mPlugin.mTimerManager.getQuadTree(world);

					if (quadTree == null) {
						quadTree = new QuadTree<>();
						mPlugin.mTimerManager.mQuadTrees.put(world.getUID(), quadTree);
					}
					quadTree.add(node);
					quadTree.getValues().add(node);
				}
			}
		}

		// Read Data File
		File dataFolder = new File(plugin.getDataFolder() + File.separator + "timer_data");
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}

		File dataFile = new File(plugin.getDataFolder() + File.separator + "timer_data", mId + ".json");
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

	public long getTimeUntilReset() {
		return TimeUnit.MILLISECONDS.toSeconds(mTimerData.mResetInterval - (System.currentTimeMillis() - mTimerData.mLastReset));
	}

	public void updateTimer(long currentTime) {
		long timeUntil = mTimerData.mResetInterval - (currentTime - mTimerData.mLastReset);
		//Bukkit.broadcastMessage(mId + ": " + TimeUnit.MILLISECONDS.toMinutes(timeUntil));
		if (timeUntil <= 0) {
			mTimerData.mLastReset += mTimerData.mResetInterval;
			mTimerData.mResetCounter++;

			// Recursive in case we need to update it multiple times to catch it up.
			updateTimer(currentTime);
		}
	}

	public void saveTimerData() {
		try {
			String path = mPlugin.getDataFolder() + File.separator + "timer_data" + File.separator + mId + ".json";
			FileUtils.writeFile(path, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(mTimerData.getAsJsonObject()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
