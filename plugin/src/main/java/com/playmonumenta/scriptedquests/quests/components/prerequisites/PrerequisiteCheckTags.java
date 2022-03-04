package com.playmonumenta.scriptedquests.quests.components.prerequisites;

import com.google.gson.JsonElement;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import java.util.Set;

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
	public boolean prerequisiteMet(QuestContext context) {
		Set<String> entityTags = context.getEntityUsedForPrerequisites().getScoreboardTags();
		return mInverted ^ entityTags.contains(mTag);
	}
}
