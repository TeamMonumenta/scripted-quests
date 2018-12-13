package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import java.util.Set;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

public class PrerequisiteCheckTags implements PrerequisiteBase {
	private boolean mInverted;
	private String mTag;

	public PrerequisiteCheckTags(JsonElement value) throws Exception {
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
