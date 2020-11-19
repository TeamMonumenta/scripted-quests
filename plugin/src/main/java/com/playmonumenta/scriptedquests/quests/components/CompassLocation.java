package com.playmonumenta.scriptedquests.quests.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CompassLocation implements QuestLocation {
	private final QuestPrerequisites mPrerequisites;
	private final String mMessage;
	private final List<Location> mWaypoints = new ArrayList<Location>();

	public CompassLocation(World world, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("locations value is not an object!");
		}

		// Reuse the same pre-requisites logic as the scripted quests
		JsonElement prereq = object.get("prerequisites");
		if (prereq == null) {
			throw new Exception("Failed to parse location prerequisites!");
		}
		mPrerequisites = new QuestPrerequisites(prereq);

		if (object.has("waypoints")) {
			JsonArray array = object.getAsJsonArray("waypoints");
			if (array == null) {
				throw new Exception("Failed to parse 'waypoints' as JSON array");
			}

			// Loop over the coordinates and add them
			Iterator<JsonElement> iter = array.iterator();
			while (iter.hasNext()) {
				JsonObject entry = iter.next().getAsJsonObject();

				// Read x coordinate
				JsonElement xElement = entry.get("x");
				int x = 0;
				if (xElement == null) {
					throw new Exception("Failed to parse location x value!");
				}
				x = xElement.getAsInt();

				// Read y coordinate
				JsonElement yElement = entry.get("y");
				int y = 0;
				if (yElement == null) {
					throw new Exception("Failed to parse location y value!");
				}
				y = yElement.getAsInt();

				// Read z coordinate
				JsonElement zElement = entry.get("z");
				int z = 0;
				if (zElement == null) {
					throw new Exception("Failed to parse location z value!");
				}
				z = zElement.getAsInt();

				mWaypoints.add(new Location(world, x + 0.5, y, z + 0.5));
			}

		} else {
			// Read x coordinate
			JsonElement xElement = object.get("x");
			int x = 0;
			if (xElement == null) {
				throw new Exception("Failed to parse location x value!");
			}
			x = xElement.getAsInt();

			// Read z coordinate
			JsonElement zElement = object.get("z");
			int z = 0;
			if (zElement == null) {
				throw new Exception("Failed to parse location z value!");
			}
			z = zElement.getAsInt();

			mWaypoints.add(new Location(world, x + 0.5, -1, z + 0.5));
		}

		// Read message
		JsonElement msgElement = object.get("message");
		if (msgElement == null) {
			throw new Exception("Failed to parse location message!");
		}
		mMessage = msgElement.getAsString();
		if (mMessage == null) {
			throw new Exception("Failed to parse location message as string!");
		}

		// Make sure waypoints list contains at least one entry
		if (mWaypoints.size() <= 0) {
			throw new Exception("Compass entry must contain at least one waypoint");
		}
	}

	public CompassLocation(QuestPrerequisites questPrereq, String message, List<Location> waypoints) {
		mPrerequisites = questPrereq;
		mMessage = message;
		for (Location l : waypoints) {
			mWaypoints.add(l);
		}
	}

	@Override
	public Location getLocation() {
		return mWaypoints.get(mWaypoints.size() - 1);
	}

	@Override
	public List<Location> getWaypoints() {
		return mWaypoints;
	}

	@Override
	public String getMessage() {
		return mMessage;
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		if (mPrerequisites == null) {
			return true;
		} else {
			return mPrerequisites.prerequisiteMet(player, null);
		}
	}
}
