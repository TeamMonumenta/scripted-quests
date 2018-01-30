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
	Set<String> mNotTags;

	public PrerequisiteCheckTags(JsonElement element) throws Exception {
		JsonArray array = element.getAsJsonArray();
		if (array == null) {
			throw new Exception("tags value is not an array!");
		}

		mTags = new HashSet<>();
		mNotTags = new HashSet<>();

		// Add all array entries
		Iterator<JsonElement> iter = array.iterator();
		while (iter.hasNext()) {
			String tag = iter.next().getAsString();
			if (tag == null) {
				throw new Exception("tag value is not a string!");
			}

			if (tag.charAt(0) == '!') {
				mNotTags.add(tag.substring(1));
			} else {
				mTags.add(tag);
			}
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		Set<String> playerTags = player.getScoreboardTags();
		if (playerTags.containsAll(mTags)) {
			Iterator<String> notTheseIterator = mNotTags.iterator();
			while (notTheseIterator.hasNext()) {
				String notThis = notTheseIterator.next();
				if (playerTags.contains(notThis)) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}
}
