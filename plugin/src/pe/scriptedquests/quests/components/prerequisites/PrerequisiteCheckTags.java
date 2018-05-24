package pe.scriptedquests.quests;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

class PrerequisiteCheckTags implements PrerequisiteBase {
	private boolean mInverted;
	private String mTag;

	PrerequisiteCheckTags(JsonElement value) throws Exception {
		String tag = value.getAsString();
		if (tag == null) {
			throw new Exception("tag value is not a string!");
		}

		if (tag.charAt(0) == '!') {
			mInverted = true;
			mTag = tag.substring(1);
		} else {
			mInverted = false;
			mTag = tag;
		}
	}

	@Override
	public boolean prerequisiteMet(Player player) {
		Set<String> playerTags = player.getScoreboardTags();
		return mInverted ^ playerTags.contains(mTag);
	}
}
