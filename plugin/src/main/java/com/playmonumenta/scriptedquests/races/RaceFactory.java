package com.playmonumenta.scriptedquests.races;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.playmonumenta.scriptedquests.managers.RaceManager;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.quests.QuestActions;
import com.playmonumenta.scriptedquests.utils.FileUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.scoreboard.Objective;
import org.bukkit.World;

/*
 * A RaceFactory object is a parsed version of a JSON race file.
 * It is used to instantiate races for players
 */
public class RaceFactory {
	private final String mName;
	private final String mLabel;
	private final Objective mScoreboard;
	private final boolean mShowStats;
	private final Location mStart;
	private final List<RaceWaypoint> mWaypoints = new ArrayList<RaceWaypoint>();
	private final List<RaceTime> mTimes = new ArrayList<RaceTime>();
	private final QuestActions mLoseActions;
	private final Plugin mPlugin;
	private final RaceManager mManager;

	public RaceFactory(String fileLocation, Plugin plugin, RaceManager manager) throws Exception {
		String content = FileUtils.readFile(fileLocation);
		if (content == null || content.isEmpty()) {
			throw new Exception("Race file '" + fileLocation + "' could not be read!");
		}

		Gson gson = new Gson();
		JsonObject object = gson.fromJson(content, JsonObject.class);
		if (object == null) {
			throw new Exception("Failed to parse file '" + fileLocation + "' as JSON object");
		}

		// name
		JsonElement name = object.get("name");
		if (name == null) {
			throw new Exception("'name' entry for race '" + fileLocation + "' is required");
		}
		mName = name.getAsString();
		if (mName == null) {
			throw new Exception("Failed to parse 'name' name for file '" +
			fileLocation + "' as string");
		}

		// label
		JsonElement label = object.get("label");
		if (label == null) {
			throw new Exception("'label' entry for race '" + fileLocation + "' is required");
		}
		mLabel = label.getAsString();
		if (mLabel == null) {
			throw new Exception("Failed to parse 'label' label for file '" +
			fileLocation + "' as string");
		}

		// scoreboard
		JsonElement scoreboard = object.get("scoreboard");
		if (scoreboard != null) {
			String objectiveStr = scoreboard.getAsString();
			if (objectiveStr == null) {
				throw new Exception("Failed to parse 'scoreboard' label for file '" +
				fileLocation + "' as string");
			}
			mScoreboard = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveStr);
			if (mScoreboard == null) {
				throw new Exception("Scoreboard objective '" + objectiveStr + "' does not exist");
			}
		} else {
			mScoreboard = null;
		}

		// show_stats
		JsonElement show_stats = object.get("show_stats");
		if (show_stats == null) {
			throw new Exception("'show_stats' entry for race '" + fileLocation + "' is required");
		}
		Boolean stats = show_stats.getAsBoolean();
		if (stats == null) {
			throw new Exception("Failed to parse 'show_stats' label for file '" +
			fileLocation + "' as boolean");
		}
		mShowStats = stats;

		// start
		JsonElement start = object.get("start");
		if (start == null) {
			throw new Exception("'start' entry for race '" + fileLocation + "' is required");
		}
		mStart = parseLocation(start, Bukkit.getWorlds().get(0), "start", fileLocation);

		// waypoints
		JsonArray waypointsArray = object.getAsJsonArray("waypoints");
		if (waypointsArray == null) {
			throw new Exception("Failed to parse 'waypoints' for file '"
			+ fileLocation + "' as JSON array");
		}
		Iterator<JsonElement> waypointsIter = waypointsArray.iterator();
		while (waypointsIter.hasNext()) {
			JsonElement entry = waypointsIter.next();
			mWaypoints.add(new RaceWaypoint(entry));
		}

		// times
		JsonArray timesArray = object.getAsJsonArray("times");
		if (timesArray == null) {
			throw new Exception("Failed to parse 'times' for file '"
			+ fileLocation + "' as JSON array");
		}
		Iterator<JsonElement> timesIter = timesArray.iterator();
		while (timesIter.hasNext()) {
			JsonElement entry = timesIter.next();
			mTimes.add(new RaceTime(entry));
		}

		// lose_actions (optional)
		JsonElement loseActionsElement = object.get("lose_actions");
		if (loseActionsElement != null) {
			// Actions should not use NPC dialog or rerun_components since they make no sense here
			mLoseActions = new QuestActions("REPORT_THIS_BUG", "REPORT_THIS_BUG", null, 0, loseActionsElement);
		} else {
			mLoseActions = null;
		}

		mPlugin = plugin;
		mManager = manager;
	}

	public String getLabel() {
		return mLabel;
	}

	private Location parseLocation(JsonElement element, World world, String label, String fileLocation) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("'" + label + "' entry for race '" + fileLocation + "' is not an object");
		}

		// x
		JsonElement xElement = object.get("x");
		double x = 0;
		if (xElement != null) {
			x = xElement.getAsDouble();
		} else {
			throw new Exception("waypoints entry missing x value!");
		}

		// y
		JsonElement yElement = object.get("y");
		double y = 0;
		if (yElement != null) {
			y = yElement.getAsDouble();
		} else {
			throw new Exception("waypoints entry missing y value!");
		}

		// z
		JsonElement zElement = object.get("z");
		double z = 0;
		if (zElement != null) {
			z = zElement.getAsDouble();
		} else {
			throw new Exception("waypoints entry missing z value!");
		}

		// yaw
		JsonElement yawElement = object.get("yaw");
		float yaw = 0;
		if (yawElement != null) {
			yaw = yawElement.getAsFloat();
		} else {
			throw new Exception("waypoints entry missing yaw value!");
		}

		// pitch
		JsonElement pitchElement = object.get("pitch");
		float pitch = 0;
		if (pitchElement != null) {
			pitch = pitchElement.getAsFloat();
		} else {
			throw new Exception("waypoints entry missing pitch value!");
		}

		return new Location(world, x, y, z, yaw, pitch);
	}

	public Race createRace(Player player) {
		return new Race(mPlugin, mManager, player, mName, mLabel, mScoreboard,
		                mShowStats, mStart, mWaypoints, mTimes, mLoseActions);
	}
}
