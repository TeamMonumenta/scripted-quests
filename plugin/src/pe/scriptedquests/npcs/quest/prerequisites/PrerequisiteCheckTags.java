package pe.scriptedquests.npcs.quest.prerequisites;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PrerequisiteCheckTags implements PrerequisiteBase {
	Set<String> mTags;

	public PrerequisiteCheckTags(JsonElement element) throws Exception {
		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("actions value is not an array!");
		}

		mTags = new HashSet<>();

		// Add all array entries
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			String tag = iter.next().getAsString();
			if (tag == null) {
				throw new Exception("tag value is not a string!");
			}

			mTags.add(tag);
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		Set<String> playerTags = player.getScoreboardTags();
		return playerTags.containsAll(mTags);
	}
}
