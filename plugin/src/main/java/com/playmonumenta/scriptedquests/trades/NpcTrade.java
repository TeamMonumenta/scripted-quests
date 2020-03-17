package com.playmonumenta.scriptedquests.trades;

import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.scriptedquests.quests.components.QuestPrerequisites;

public class NpcTrade implements Comparable<NpcTrade> {
	private final int mIndex;
	private final QuestPrerequisites mPrerequisites;

	public NpcTrade(JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("trade value is not an object!");
		}

		// index
		JsonElement indexElement = object.get("index");
		if (indexElement != null) {
			mIndex = indexElement.getAsInt();
		} else {
			throw new Exception("trade entry missing index value!");
		}

		// prerequisites
		JsonElement prereqElement = object.get("prerequisites");
		if (prereqElement != null) {
			mPrerequisites = new QuestPrerequisites(prereqElement);
		} else {
			throw new Exception("trade entry missing mPrerequisites value!");
		}

		// Iterate through the remaining keys and throw an error if any are found
		Set<Entry<String, JsonElement>> entries = object.entrySet();
		for (Entry<String, JsonElement> ent : entries) {
			String key = ent.getKey();

			if (!key.equals("index") && !key.equals("prerequisites")) {
				throw new Exception("Unknown trade key: " + key);
			}
		}
	}

	public int getIndex() {
		return mIndex;
	}

	public boolean prerequisiteMet(Player player, Entity npc) {
		return mPrerequisites.prerequisiteMet(player, npc);
	}

	@Override
	public int compareTo(NpcTrade other) {
		// compareTo should return < 0 if this is supposed to be
		// less than other, > 0 if this is supposed to be greater than
		// other and 0 if they are supposed to be equal
		return mIndex < other.mIndex ? -1 : mIndex == other.mIndex ? 0 : 1;
	}
}
