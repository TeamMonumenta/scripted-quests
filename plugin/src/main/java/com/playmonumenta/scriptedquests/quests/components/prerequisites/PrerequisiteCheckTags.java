package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import java.util.Set;

import org.bukkit.entity.Entity;

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
	public boolean prerequisiteMet(Entity entity, Entity npcEntity) {
		Set<String> entityTags = entity.getScoreboardTags();
		return mInverted ^ entityTags.contains(mTag);
	}
}
