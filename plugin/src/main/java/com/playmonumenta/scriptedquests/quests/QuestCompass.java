package com.playmonumenta.scriptedquests.quests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.CompassLocation;
import com.playmonumenta.scriptedquests.quests.components.QuestLocation;

public class QuestCompass {
	private String mQuestName;
	private ArrayList<QuestLocation> mMarkers = new ArrayList<QuestLocation>();

	public QuestCompass(World world, JsonObject object) throws Exception {
		// Read the quest name
		JsonElement questName = object.get("quest_name");
		if (questName == null) {
			throw new Exception("'quest_name' entry is required");
		}
		mQuestName = questName.getAsString();
		if (mQuestName == null) {
			throw new Exception("Failed to parse 'quest_name' as string");
		}

		// Read the locations
		JsonElement locations = object.get("locations");
		if (locations == null) {
			throw new Exception("'locations' entry is required");
		}
		JsonArray array = locations.getAsJsonArray();
		if (array == null) {
			throw new Exception("Failed to parse 'locations' as JSON array");
		}

		// Loop over the locations and add them
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			JsonElement entry = iter.next();

			mMarkers.add(new QuestLocation(world, entry));
		}
	}

	public List<CompassLocation> getMarkers(Player player) {
		List<CompassLocation> availableMarkers = new ArrayList<CompassLocation>();

		for (QuestLocation marker : mMarkers) {
			if (marker.prerequisiteMet(player)) {
				availableMarkers.add(marker);
			}
		}

		return availableMarkers;
	}

	public ArrayList<QuestLocation> getMarkers() {
		return mMarkers;
	}

	public String getQuestName() {
		return mQuestName;
	}
}
